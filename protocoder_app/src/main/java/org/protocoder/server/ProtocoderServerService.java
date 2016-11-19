package org.protocoder.server;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.protocoder.appinterpreter.AppRunnerCustom;
import org.protocoder.events.Events;
import org.protocoder.events.EventsProxy;
import org.protocoder.helpers.ProtoAppHelper;
import org.protocoder.gui.settings.ProtocoderSettings;
import org.protocoder.helpers.ProtoScriptHelper;
import org.protocoderrunner.api.PDevice;
import org.protocoderrunner.base.network.NetworkUtils;
import org.protocoderrunner.base.utils.AndroidUtils;
import org.protocoderrunner.base.utils.MLog;
import org.protocoderrunner.models.Project;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class ProtocoderServerService extends Service {

    private final String TAG = ProtocoderServerService.class.getSimpleName();

    private final int NOTIFICATION_ID = 58592;
    private static final String SERVICE_CLOSE = "service_close";

    private NotificationManager mNotifManager;
    private PendingIntent mRestartPendingIntent;
    private Toast mToast;
    private EventsProxy mEventsProxy;

    /*
     * Servers
     */
    private ProtocoderHttpServer protocoderHttpServer;
    private ProtocoderFtpServer protocoderFtpServer;
    private ProtocoderWebsocketServer protocoderWebsockets;

    private Gson gson = new Gson();
    private int counter = 0;
    private Project mProjectRunning;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.d(TAG, "onStartCommand");

        if (intent != null) {
            AndroidUtils.debugIntent(TAG, intent);
            if (intent.getAction() == SERVICE_CLOSE) stopSelf();
        }

        return Service.START_STICKY;
    }

    BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MLog.d(TAG, "received action: " + intent.getAction());
            if (intent.getAction().equals(SERVICE_CLOSE)) {
                //ProtocoderServerService.this.stopSelf();
                //mNotifManager.cancel(NOTIFICATION_SERVER_ID);
            }
        }
    };

    Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(ProtocoderServerService.this, "lalll", Toast.LENGTH_LONG);
                    Looper.loop();
                }
            }.start();
            //          handlerToast.post(runnable);

            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mRestartPendingIntent);
            mNotifManager.cancelAll();

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);

            throw new RuntimeException(ex);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLog.d(TAG, "network service created");

        final AppRunnerCustom appRunner = new AppRunnerCustom(this).initDefaultObjects();

        /*
         * Init the event proxy
         */
        mEventsProxy = new EventsProxy();

        EventBus.getDefault().register(this);

        Intent notificationIntent = new Intent(this, ProtocoderServerService.class).setAction(SERVICE_CLOSE);
        PendingIntent pendingIntent = PendingIntent.getService(this, (int) System.currentTimeMillis(), notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(org.protocoderrunner.R.drawable.protocoder_icon)
                .setContentTitle("Protocoder").setContentText("Running service ")
                .setOngoing(false)
                .addAction(org.protocoderrunner.R.drawable.ic_action_stop, "stop", pendingIntent)
                //.setDeleteIntent(pendingIntent)
                .setContentInfo("1 Connection");

        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);

        protocoderHttpServer = new ProtocoderHttpServer(this, ProtocoderSettings.HTTP_PORT);

        try {
            protocoderWebsockets = new ProtocoderWebsocketServer(this, ProtocoderSettings.WEBSOCKET_PORT);
            protocoderWebsockets.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {

                HashMap data = new HashMap();
                data.put("module", "device");
                HashMap info = new HashMap();
                data.put("info", info);

                // device
                HashMap device = new HashMap();
                PDevice.DeviceInfo deviceInfo = appRunner.pDevice.info();
                device.put("type", appRunner.pDevice.type());
                device.put("model name", deviceInfo.model);
                device.put("manufacturer", deviceInfo.manufacturer);

                // screen
                HashMap screen = new HashMap();
                screen.put("orientation", appRunner.pDevice.orientation());
                screen.put("screen", appRunner.pDevice.isScreenOn());
                screen.put("screen resolution", deviceInfo.screenWidth + " x " + deviceInfo.screenHeight);
                screen.put("screen dpi", deviceInfo.screenDpi);
                screen.put("brightness", appRunner.pDevice.brightness());

                // others
                HashMap other = new HashMap();
                other.put("battery level", appRunner.pDevice.battery());
                other.put("memory", appRunner.pDevice.memory().summary());

                // network
                HashMap network = new HashMap();
                network.put("network available", appRunner.pNetwork.isNetworkAvailable());
                network.put("wifi enabled", appRunner.pNetwork.isWifiEnabled());
                network.put("network type", appRunner.pNetwork.getNetworkType());
                network.put("ip", appRunner.pNetwork.ipAddress());
                network.put("rssi", appRunner.pNetwork.wifiInfo().getRssi());
                network.put("ssid", appRunner.pNetwork.wifiInfo().getSSID());

                // scripts info (name of the current running project)
                HashMap script = new HashMap();

                String name = "none";
                if (mProjectRunning != null) name = mProjectRunning.getName();
                script.put("running script", name);

                info.put("device", device);
                info.put("screen", screen);
                info.put("other", other);
                info.put("network", network);
                info.put("script", script);

                String jsonObject = gson.toJson(data);

                protocoderWebsockets.send(jsonObject);
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(r, 0);

        //protocoderFtpServer = new ProtocoderFtpServer(this);

        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        fileObserver.startWatching();

        // register log broadcast
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction("org.protocoder.intent.CONSOLE");
        registerReceiver(logBroadcastReceiver, filterSend);

        // register a broadcast to receive the notification commands
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_CLOSE);
        registerReceiver(mNotificationReceiver, filter);
        startStopActivityBroadcastReceiver();

        viewsUpdate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MLog.d(TAG, "service destroyed");
        mEventsProxy.stop();
        protocoderHttpServer.stop();
        protocoderWebsockets.stop();

        // unregisterReceiver(mNotificationReceiver);

        unregisterReceiver(connectivityChangeReceiver);
        unregisterReceiver(mNotificationReceiver);
        unregisterReceiver(logBroadcastReceiver);
        unregisterReceiver(stopActivitiyBroadcastReceiver);
        unregisterReceiver(viewsUpdateBroadcastReceiver);

        fileObserver.stopWatching();

        EventBus.getDefault().unregister(this);
    }

    /*
     * Network Connectivity listener
     */
    BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AndroidUtils.debugIntent("connectivityChangerReceiver", intent);

            // check if there is mContext WIFI connection or we can connect via USB
            if (NetworkUtils.getLocalIpAddress(ProtocoderServerService.this).equals("-1")) {
                MLog.d(TAG, "No WIFI, still you can hack via USB using the adb command");
                EventBus.getDefault().post(new Events.Connection("none", ""));

            } else {
                MLog.d(TAG, "Hack via your browser @ http://" + NetworkUtils.getLocalIpAddress(ProtocoderServerService.this) + ":" + ProtocoderSettings.HTTP_PORT);
                String ip = NetworkUtils.getLocalIpAddress(ProtocoderServerService.this) + ":" + ProtocoderSettings.HTTP_PORT;
                EventBus.getDefault().post(new Events.Connection("wifi", ip));
            }
        }
    };

    /*
     * FileObserver to notify when projects are added or removed
     */
    FileObserver fileObserver = new FileObserver(ProtocoderSettings.getBaseDir(), FileObserver.CREATE| FileObserver.DELETE) {

        @Override
        public void onEvent(int event, String file) {
            if ((FileObserver.CREATE & event) != 0) {
                MLog.d(TAG, "File created [" + ProtocoderSettings.getBaseDir() + "/" + file + "]");
            } else if ((FileObserver.DELETE & event) != 0) {
                MLog.d(TAG, "File deleted [" + ProtocoderSettings.getBaseDir() + "/" + file + "]");
            }
        }
    };

    /*
     * Notification that show if the server is ON
     */
    private void createNotification() {
        /*
        //create pending intent that will be triggered if the notification is clicked
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_CLOSE);
        // registerReceiver(mNotificationReceiver, filter);

        Intent stopIntent = new Intent(SERVICE_CLOSE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(org.protocoderrunner.R.drawable.protocoder_icon)
                .setContentTitle("Protocoder").setContentText("Running service ")
                .setOngoing(false)
                .addAction(org.protocoderrunner.R.drawable.ic_action_stop, "stop", pendingIntent)
                .setDeleteIntent(pendingIntent);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for
        // navigating backward from the Activity leads out your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(AppRunnerActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_SERVER_ID, mBuilder.build());

        Thread.setDefaultUncaughtExceptionHandler(handler);
        */
    }

    /**
     * Events
     *
     * - Start app
     * - Stop service
     *
     */

    /**
     * send logs to WEBIDE
     */
    BroadcastReceiver logBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MLog.d(TAG, intent.getAction());

            HashMap hashMap = new HashMap();
            hashMap.put("module", "console");
            hashMap.put("action", intent.getStringExtra("action"));
            hashMap.put("time", intent.getStringExtra("time"));
            hashMap.put("data", intent.getStringExtra("data"));
            String jsonObject = gson.toJson(hashMap);

            protocoderWebsockets.send(jsonObject);
        }
    };

    @Subscribe
    public void onEventMainThread(Events.ProjectEvent e) {
        MLog.d(TAG, "event -> " + e.getAction());

        String action = e.getAction();
        if (action.equals(Events.PROJECT_RUN)) {
            ProtoAppHelper.launchScript(getApplicationContext(), e.getProject());
            mProjectRunning = e.getProject();
        } else if (action.equals(Events.PROJECT_STOP_ALL_AND_RUN)) {
            // ProtoScriptHelper.stop_all_scripts();
            Intent i = new Intent("org.protocoderrunner.intent.CLOSE");
            sendBroadcast(i);
            ProtoAppHelper.launchScript(getApplicationContext(), e.getProject());

        } else if (action.equals(Events.PROJECT_STOP_ALL)) {
            Intent i = new Intent("org.protocoderrunner.intent.CLOSE");
            sendBroadcast(i);
        } else if (action.equals(Events.PROJECT_SAVE)) {
            //Project p = evt.getProject();
            //mProtocoder.protoScripts.refresh(p.getFolder(), p.getName());
        } else if (action.equals(Events.PROJECT_NEW)) {
            //MLog.d(TAG, "creating new project " + evt.getProject().getName());
            //mProtocoder.protoScripts.createProject("projects", evt.getProject().getName());
        } else if (action.equals(Events.PROJECT_UPDATE)) {
            //mProtocoder.protoScripts.listRefresh();
        } else if (action.equals(Events.PROJECT_EDIT)) {
            MLog.d(TAG, "edit " + e.getProject().getName());

            ProtoAppHelper.launchEditor(getApplicationContext(), e.getProject());
        }
    }

    @Subscribe
    public void onEventMainThread(Events.ExecuteCodeEvent e) {
        Intent i = new Intent("org.protocoderrunner.intent.EXECUTE_CODE");
        i.putExtra("code", e.getCode());
        sendBroadcast(i);
    }

    //stop service
    @Subscribe
    public void onEventMainThread(Events.SelectedProjectEvent e) {
        // stopSelf();
    }

    /**
     * Receiving order to close the apprunneractivity
     */
    public void startStopActivityBroadcastReceiver() {
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction("org.protocoder.intent.CLOSED");
        registerReceiver(stopActivitiyBroadcastReceiver, filterSend);
    }

    BroadcastReceiver stopActivitiyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MLog.d(TAG, "stop_all 2");
            mProjectRunning = null;
        }
    };


    /**
     * Receiving order to execute line of code
     */
    public void viewsUpdate() {
        MLog.d("registerreceiver", "sending event");

        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction("org.protocoder.intent.VIEWS_UPDATE");
        registerReceiver(viewsUpdateBroadcastReceiver, filterSend);
    }

    BroadcastReceiver viewsUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String views = intent.getStringExtra("views");
            MLog.d(TAG, "views" + views);
            protocoderHttpServer.setViews(views);
        }
    };

}