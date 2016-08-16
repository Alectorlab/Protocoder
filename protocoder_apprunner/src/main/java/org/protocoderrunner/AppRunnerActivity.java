/*
* Part of Protocoder http://www.protocoder.org
* A prototyping platform for Android devices
*
* Copyright (C) 2013 Victor Diaz Barrales victormdb@gmail.com
*
* Protocoder is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Protocoder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Protocoder. If not, see <http://www.gnu.org/licenses/>.
*/

package org.protocoderrunner;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.protocoderrunner.api.PDevice;
import org.protocoderrunner.api.PMedia;
import org.protocoderrunner.api.network.PBluetooth;
import org.protocoderrunner.api.sensors.PNfc;
import org.protocoderrunner.apprunner.AppRunnerSettings;
import org.protocoderrunner.base.BaseActivity;
import org.protocoderrunner.base.gui.DebugFragment;
import org.protocoderrunner.base.utils.MLog;
import org.protocoderrunner.base.utils.StrUtils;
import org.protocoderrunner.events.Events;
import org.protocoderrunner.models.Project;

import java.util.ArrayList;

public class AppRunnerActivity extends BaseActivity {

    private static final String TAG = AppRunnerActivity.class.getSimpleName();

    private Context mContext;
    private AppRunnerFragment mAppRunnerFragment;

    /*
     * Events
     */
    private PNfc.onNFCListener                  onNFCListener;
    private PNfc.onNFCWrittenListener           onNFCWrittenListener;
    private PBluetooth.onBluetoothListener      onBluetoothListener;
    private PMedia.onVoiceRecognitionListener   onVoiceRecognitionListener;

    // ui fragment dependent
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 55;

    /*
     * Keyboard handling
     */
    private PDevice.onKeyListener   onKeyListener;
    public boolean ignoreVolumeEnabled = false;
    public boolean ignoreBackEnabled = false;

    /*
     * UI stuff
     */
    private DebugFragment   mDebugFragment;

    // project settings
    private boolean mSettingScreenAlwaysOn;
    private boolean mSettingWakeUpScreen;
    private boolean eventBusRegistered = false;
    private boolean debugFramentIsVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        registerEventBus();

        Intent intent = getIntent();

        // if intent is empty => finish
        if (intent == null) finish();

        // if is a service with start it and finish this activity
        if (intent.getBooleanExtra("isService", false)) {
            Intent i = new Intent(this, AppRunnerService.class);
            i.putExtras(intent);
            this.startService(i);
            finish();
        }

        // settings
        mSettingScreenAlwaysOn   = intent.getBooleanExtra(Project.SETTINGS_SCREEN_ALWAYS_ON, false);
        mSettingWakeUpScreen     = intent.getBooleanExtra(Project.SETTINGS_SCREEN_WAKEUP, false);

        // the actual code
        String prefix   = intent.getStringExtra(Project.PREFIX);
        String code     = intent.getStringExtra(Project.INTENTCODE);
        String postfix  = intent.getStringExtra(Project.POSTFIX);

        // send bundle to the fragment
        Bundle bundle = new Bundle();
        bundle.putString(Project.NAME, intent.getStringExtra(Project.NAME));
        bundle.putString(Project.FOLDER, intent.getStringExtra(Project.FOLDER));
        bundle.putString(Project.PREFIX, prefix);
        bundle.putString(Project.INTENTCODE, code);
        bundle.putString(Project.POSTFIX, postfix);

        // Set the Activity UI
        setContentView(R.layout.apprunner_activity);
        setupActivity();

        // Add debug fragment
        if (AppRunnerSettings.DEBUG) addDebugFragment();

        // add AppRunnerFragment
        mAppRunnerFragment = AppRunnerFragment.newInstance(bundle);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        FrameLayout fl = (FrameLayout) findViewById(R.id.apprunner_fragment);
        ft.add(fl.getId(), mAppRunnerFragment, String.valueOf(fl.getId()));
        ft.commit();

        //TODO change to events
        //IDEcommunication.getInstance(this).ready(true);
    }

    @Override
    protected void setupActivity() {
        super.setupActivity();

        // wake up the device if intent says so
        if (mSettingWakeUpScreen) {
            final Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setScreenAlwaysOn(mSettingScreenAlwaysOn);

    }

    @Override
    protected void onResume() {
        super.onResume();

        registerEventBus();

        // NFC
        if (nfcSupported) mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

        // broadcast to start/stop the activity
        startStopActivityBroadcastReceiver();
        executeCodeActivityBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterEventBus();

        if (nfcSupported) mAdapter.disableForegroundDispatch(this);
        unregisterReceiver(stopActivitiyBroadcastReceiver);
        unregisterReceiver(executeCodeActivitiyBroadcastReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Intent i = new Intent("org.protocoder.intent.CLOSED");
        sendBroadcast(i);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void registerEventBus() {
        if (!eventBusRegistered) {
            EventBus.getDefault().register(this);
            eventBusRegistered = true;
        }
    }

    public void unregisterEventBus() {
        EventBus.getDefault().unregister(this);
        eventBusRegistered = false;
    }

    private void addDebugFragment() {
        mDebugFragment = DebugFragment.newInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        FrameLayout fl = (FrameLayout) findViewById(R.id.debug_fragment);
        ft.add(fl.getId(), mDebugFragment, String.valueOf(fl.getId()));
        ft.commit();

        debugFramentIsVisible = true;
    }

    private void removeDebugFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(mDebugFragment);
        ft.commit();

        debugFramentIsVisible = false;
    }

    /**
     * NFC stuf
	 */
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private boolean nfcSupported;
    private boolean nfcInit = false;
    public boolean isCodeExecutedShown;

    public void initializeNFC() {

        if (nfcInit == false) {
            PackageManager pm = getPackageManager();
            nfcSupported = pm.hasSystemFeature(PackageManager.FEATURE_NFC);

            if (nfcSupported == false) {
                return;
            }

            // when is in foreground
            MLog.d(TAG, "starting NFC");
            mAdapter = NfcAdapter.getDefaultAdapter(this);

            // PedingIntent will be delivered to this activity
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            // Setup an intent filter for all MIME based dispatches
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("*/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                throw new RuntimeException("fail", e);
            }
            mFilters = new IntentFilter[]{ ndef, };

            // Setup a tech list for all NfcF tags
            mTechLists = new String[][]{new String[]{NfcF.class.getName()}};
            nfcInit = true;
        }
    }


    /**
     * Listen to NFC incomming data
     */
    @Override
    public void onNewIntent(Intent intent) {
        MLog.d(TAG, "New intent " + intent);

        if (intent.getAction() != null) {
            MLog.d(TAG, "Discovered tag with intent: " + intent);

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String nfcID = StrUtils.bytetostring(tag.getId());

            // if there is a message waiting to be written
            if (PNfc.nfcMsg != null) {
                MLog.d(TAG, "->" + PNfc.nfcMsg);
                PNfc.writeTag(this, tag, PNfc.nfcMsg);
                onNFCWrittenListener.onNewTag();
                onNFCWrittenListener = null;
                PNfc.nfcMsg = null;

            // read the nfc tag info
            } else {
                // get NDEF tag details
                Ndef ndefTag = Ndef.get(tag);
                if (ndefTag == null) {
                    return;
                }

                int size = ndefTag.getMaxSize(); // tag size
                boolean writable = ndefTag.isWritable(); // is tag writable?
                String type = ndefTag.getType(); // tag type

                String nfcMessage = "";

                // get NDEF message details
                NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
                if (ndefMesg != null) {
                    NdefRecord[] ndefRecords = ndefMesg.getRecords();
                    int len = ndefRecords.length;
                    String[] recTypes = new String[len]; // will contain the
                    // NDEF record types
                    String[] recPayloads = new String[len]; // will contain the
                    // NDEF record types
                    for (int i = 0; i < len; i++) {
                        recTypes[i] = new String(ndefRecords[i].getType());
                        recPayloads[i] = new String(ndefRecords[i].getPayload());
                        MLog.d(TAG, "qq " + i + " " + recTypes[i] + " " + recPayloads[i]);

                    }
                    nfcMessage = recPayloads[0];
                }
                onNFCListener.onNewTag(nfcID, nfcMessage);
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // AndroidUtils.dumpMotionEvent(event);

        return super.onGenericMotionEvent(event);
    }

    /**
     * key listeners
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (AppRunnerSettings.DEBUG && keyCode == 25) {
            if (debugFramentIsVisible) {
                removeDebugFragment();
            } else {
                addDebugFragment();
            }
        }
        
        if (onKeyListener != null) {
            onKeyListener.onKeyDown(event);
            onKeyListener.onKeyEvent(event);
        }

        // check if back key or volume keys are disabled
        MLog.d(TAG, "checkbackkey " + checkBackKey(keyCode));

        if (checkBackKey(keyCode) || checkVolumeKeys(keyCode)) return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (onKeyListener != null) {
            onKeyListener.onKeyUp(event);
            onKeyListener.onKeyEvent(event);
        }

        // check if back key or volume keys are disabled
        if (checkBackKey(keyCode) || checkVolumeKeys(keyCode)) return true;

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_R:
                    finish();
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }


    /**
     * Menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                overridePendingTransition(R.anim.splash_slide_in_anim_reverse_set, R.anim.splash_slide_out_anim_reverse_set);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it
            // could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            for (String _string : matches) {
                MLog.d(TAG, "" + _string);

            }
            onVoiceRecognitionListener.onNewResult(matches.get(0));

            //TODO disabled
        }

        if (onBluetoothListener != null) {
            onBluetoothListener.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addOnKeyListener(PDevice.onKeyListener onKeyListener2) { onKeyListener = onKeyListener2; }

    public void addNFCReadListener(PNfc.onNFCListener onNFCListener2) { onNFCListener = onNFCListener2; }

    public void addNFCWrittenListener(PNfc.onNFCWrittenListener onNFCWrittenListener2) { onNFCWrittenListener = onNFCWrittenListener2; }

    public void addBluetoothListener(PBluetooth.onBluetoothListener onBluetoothListener2) { onBluetoothListener = onBluetoothListener2; }

    public void addVoiceRecognitionListener(PMedia.onVoiceRecognitionListener onVoiceRecognitionListener2) { onVoiceRecognitionListener = onVoiceRecognitionListener2; }

    public boolean checkBackKey(int keyCode) {
        if (ignoreBackEnabled && keyCode == KeyEvent.KEYCODE_BACK) return true;
        else return false;
    }

    public boolean checkVolumeKeys(int keyCode) {
        if (ignoreVolumeEnabled && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        } else {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermission(String p) {
        boolean permission = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
        MLog.d(TAG, p + " " + permission);

        return permission;
    }

    /**
     * Activity dependent events
     */
    @Subscribe
    public void onEventMainThread(Events.LogEvent e) {
        Intent i = new Intent("org.protocoder.intent.CONSOLE");

        String action = e.getAction();
        String data = e.getData();

        i.putExtra("action", action);
        i.putExtra("data", data);
        sendBroadcast(i);

        MLog.d("action", action);
        if ((action == "log_error" || action == "log_permission_error") && !debugFramentIsVisible) addDebugFragment();
        else if (action == "show") addDebugFragment();
        else if (action == "hide") removeDebugFragment();
    }

    /**
     * Receiving order to close the activity
     */
    public void startStopActivityBroadcastReceiver() {
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction("org.protocoderrunner.intent.CLOSE");
        registerReceiver(stopActivitiyBroadcastReceiver, filterSend);
    }

    BroadcastReceiver stopActivitiyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    /**
     * Receiving order to close the activity
     */
    public void executeCodeActivityBroadcastReceiver() {
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction("org.protocoderrunner.intent.EXECUTE_CODE");
        registerReceiver(executeCodeActivitiyBroadcastReceiver, filterSend);
    }

    BroadcastReceiver executeCodeActivitiyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String code = intent.getStringExtra("code");

            mAppRunnerFragment.getAppRunner().interp.eval(code);

            if (mAppRunnerFragment.liveCoding != null) {
                mAppRunnerFragment.liveCoding.write(code);
            }

        }
    };


}
