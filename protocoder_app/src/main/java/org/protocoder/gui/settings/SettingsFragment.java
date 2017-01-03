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

package org.protocoder.gui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.protocoder.R;
import org.protocoder.gui.AboutActivity;
import org.protocoder.gui.LicenseActivity;
import org.protocoder.helpers.ProtoSettingsHelper;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SettingsFragment extends PreferenceFragment {

    protected static final String TAG = SettingsFragment.class.getSimpleName();
    private Context mContext;
    private NewUserPreferences mUserPreferences;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserPreferences = NewUserPreferences.getInstance();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    //twostatepreference(boolean)->action/action edittextpreference(text)->action preference->action
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mContext = getActivity();


        final EditTextPreference prefDeviceId = (EditTextPreference) findPreference("device_id");
        prefDeviceId.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefDeviceId.setText((String) newValue);
                mUserPreferences.set("device_id", (String) newValue).save();
                return false;
            }
        });
        prefDeviceId.setText((String) NewUserPreferences.getInstance().get("device_id"));

        // Screen always on mode
        final TwoStatePreference prefScreenOn = (TwoStatePreference) findPreference("screen_always_on");
        prefScreenOn.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("screen_always_on", isChecked).save();
                return true;
            }
        });
        prefScreenOn.setChecked((Boolean) mUserPreferences.get("screen_always_on"));

        // Start servers on launch
        final TwoStatePreference prefStartServers = (TwoStatePreference) findPreference("servers_enabled_on_start");
        prefStartServers.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("servers_enabled_on_start", isChecked).save();
                return true;
            }
        });
        prefStartServers.setChecked((Boolean) mUserPreferences.get("servers_enabled_on_start"));


        // Notify new version
        final TwoStatePreference prefNewVersionCheck = (TwoStatePreference) findPreference("notify_new_version");
        prefNewVersionCheck.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("notify_new_version", isChecked).save();
                return true;
            }
        });
        prefNewVersionCheck.setChecked((Boolean) mUserPreferences.get("notify_new_version"));


        // Notify new version
        final TwoStatePreference prefSendUsageLog = (TwoStatePreference) findPreference("send_usage_log");
        prefSendUsageLog.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("send_usage_log", isChecked).save();
                return true;
            }
        });
        prefSendUsageLog.setChecked((Boolean) mUserPreferences.get("send_usage_log"));


        // WebIDE mode
        final TwoStatePreference prefWebIdeMode = (TwoStatePreference) findPreference("webide_mode");
        prefWebIdeMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("webide_mode", isChecked).save();
                return true;
            }
        });
        prefWebIdeMode.setChecked((Boolean) mUserPreferences.get("webide_mode"));


        // Launch on device boot mode
        final TwoStatePreference prefLaunchOnBoot = (TwoStatePreference) findPreference("launch_on_device_boot");
        prefLaunchOnBoot.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean isChecked = (Boolean) o;
                mUserPreferences.set("launch_on_device_boot", isChecked).save();
                return true;
            }
        });
        prefLaunchOnBoot.setChecked((Boolean) mUserPreferences.get("launch_on_device_boot"));


        final EditTextPreference prefLaunchScript = (EditTextPreference) findPreference("launch_script_on_app_launch");
        prefLaunchScript.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefLaunchScript.setText((String) newValue);
                mUserPreferences.set("launch_on_device_boot", (String) newValue).save();
                return false;
            }
        });
        prefLaunchScript.setText((String) NewUserPreferences.getInstance().get("launch_script_on_app_launch"));

        // Column mode
        final TwoStatePreference prefAppsInColumnMode = (TwoStatePreference) findPreference("apps_in_list_mode");
        if (prefAppsInColumnMode != null) {
            prefAppsInColumnMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean isChecked = (Boolean) o;
                    mUserPreferences.set("apps_in_list_mode", isChecked).save();
                    return true;
                }
            });
        }
        prefAppsInColumnMode.setChecked((Boolean) mUserPreferences.get("apps_in_list_mode"));






        /*
        final EditTextPreference appColor = (EditTextPreference) findPreference("app_color");
        appColor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appColor.setText((String) newValue);
                return false;
            }
        });
        appColor.setText((String) NewUserPreferences.getInstance().get("app_color"));
        */

        Preference btnShowLicenses = findPreference("licenses_detail");
        btnShowLicenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(getActivity(), LicenseActivity.class));
                return true;
            }
        });

        Preference btnShowAbout = findPreference("app_about");
        btnShowAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
        });

        Preference btnReinstall = findPreference("reinstall_examples");
        btnReinstall.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                final ProgressDialog progress = new ProgressDialog(getActivity());
                progress.setTitle("Reinstalling examples");
                progress.setMessage("Your examples are getting restored, wait a sec!");
                progress.setCancelable(false);
                progress.setCanceledOnTouchOutside(false);

                new AlertDialog.Builder(getActivity()).setMessage("Do you really want to reinstall the examples?")
                        .setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progress.show();

                        ProtoSettingsHelper.installExamples(getActivity(), ProtocoderSettings.EXAMPLES_FOLDER,
                                new ProtoSettingsHelper.InstallListener() {

                                    @Override
                                    public void onReady() {
                                        progress.dismiss();
                                    }
                                });
                        dialog.cancel();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();

                return true;
            }
        });


        /*
        // Connection alert mode
        final TwoStatePreference connectionAlertPreference = (TwoStatePreference) findPreference("pref_connection_alert");
        if (connectionAlertPreference != null) {
            connectionAlertPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean isChecked = (Boolean) o;
                    // mSettings.setConnectionAlert(isChecked);
                    return true;
                }
            });
        }
        */

        //load webIDE
        // TODO enable again
        /*
        final ListPreference loadEditorPreference = (ListPreference) findPreference("pref_change_editor");
        String[] editors = WebEditorManager.getInstance().listEditors();
        loadEditorPreference.setEntries(editors);
        loadEditorPreference.setEntryValues(editors);
        loadEditorPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                MLog.d(TAG, "" + newValue);
                mPrefs.edit().putString("pref_change_editor", (String) newValue).commit();
                return true;
            }
        });


        // Notify and download
        final TwoStatePreference notifyNewVersionPreference = (TwoStatePreference) findPreference("pref_notify_new_version");
        if (notifyNewVersionPreference != null) {
            notifyNewVersionPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean isChecked = (Boolean) o;
                    mPrefs.edit().putBoolean("pref_notify_new_version", isChecked).commit();
                    return true;
                }
            });
        }

        Preference btnFtp = findPreference("pref_ftp");
        btnFtp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
                alertDialog.setTitle("FTP settings");
                final View view = getActivity().getLayoutInflater().inflate(R.layout.preferences_ftp_dialog, null);
                //alertDialog.setView(R.layout.preferences_ftp_dialog);


                final EditText userName = (EditText) view.findViewById(R.id.ftp_username);
                final EditText userPassword = (EditText) view.findViewById(R.id.ftp_userpassword);
                final CheckBox check = (CheckBox) view.findViewById(R.id.ftp_enable);

                final boolean[] checked = {mSettings.getFtpChecked()};
                final String[] userNameText = {mSettings.getFtpUserName()};
                final String[] userPasswordText = {mSettings.getFtpUserPassword()};

                userName.setText(userNameText[0]);
                userPassword.setText(userPasswordText[0]);

                check.setChecked(checked[0]);

                alertDialog.setView(view);

                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        checked[0] = check.isChecked();
                        userNameText[0] = userName.getText().toString();
                        userPasswordText[0] = userPassword.getText().toString();

                        //sha-1 the userPassword to store it
                        // String saltedPassword = null;
                        // try {
                        //   saltedPassword = AndroidUtils.sha1(userPasswordText[0]);
                        //    MLog.d(TAG, " qq " + saltedPassword);

                        mSettings.setFtp(checked[0], userNameText[0], userPasswordText[0]);
                        // } catch (NoSuchAlgorithmException e) {
                        //     e.printStackTrace();
                        // } catch (UnsupportedEncodingException e) {
                        //     e.printStackTrace();
                        // }

                    }
                });

                alertDialog.show();

                return true;
            }
        });
        */

        return view;

    }

}
