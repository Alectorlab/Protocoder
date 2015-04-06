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

package org.protocoder;

import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.apprunner.api.PNetwork;
import org.protocoderrunner.apprunner.api.PUI;


public class Network {

    private final AppRunnerCustom mAppRunner;

    Network(AppRunnerCustom appRunner) {
        this.mAppRunner = appRunner;
    }

    public void checkVersion() {
        //TODO reenable
        //check if new version is available

        if (mAppRunner.pNetwork.isNetworkAvailable() && mAppRunner.protocoderApp.settings.getNewVersionCheckEnabled()) {
            mAppRunner.pNetwork.httpGet("http://www.protocoder.org/downloads/list_latest.php", new PNetwork.HttpGetCB() {
                @Override
                public void event(int eventType, String responseString) {
                    //console.log(event + " " + data);
                    String[] splitted = responseString.split(":");
                    String remoteFile = "http://www.protocoder.org/downloads/" + splitted[0];
                    String versionName = splitted[1];
                    int versionCode = Integer.parseInt(splitted[2]);

                    if (versionCode > mAppRunner.pProtocoder.versionCode()) {
                        mAppRunner.pUi.popupInfo("New version available", "The new version " + versionName + " is available in the Protocoder.org website. Do you want to get it?", "Yes!", "Later", new PUI.popupCB() {
                            @Override
                            public void event(boolean b) {
                                if (b) {
                                    mAppRunner.pDevice.openWebApp("http://www.protocoder.org#download");
                                }
                            }
                        });
                    } else {
                        //console.log("updated");
                    }
                }
            });
        }
    }
}
