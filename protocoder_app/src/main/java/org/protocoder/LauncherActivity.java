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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import org.protocoder.gui.settings.UserSettings;
import org.protocoderrunner.base.utils.StrUtils;

public class LauncherActivity extends Activity {

    Intent intent = null;
    private String TAG = LauncherActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this Activity chooses between launching the welcome installer
        // or the app it self

        SharedPreferences userDetails = getSharedPreferences("org.protocoder", MODE_PRIVATE);
        boolean firstLaunch = userDetails.getBoolean(getResources().getString(R.string.pref_is_first_launch), true);

        // uncomment to avoid first launch
        //userDetails.edit().putBoolean(getResources().getString(R.string.pref_is_first_launch), false).commit();

        Intent i = getIntent();
        boolean wasCrash = i.getBooleanExtra("wasCrash", false);
        if (wasCrash) {
            Toast.makeText(this, "The script crashed :(", Toast.LENGTH_LONG).show();
        }

        if (firstLaunch) {
            intent = new Intent(this, WelcomeActivity.class);
            UserSettings userSettings = new UserSettings(this);
            userSettings.setId(StrUtils.generateRandomString());
            userSettings.setConnectionAlert(false);
        } else {
            intent = new Intent(this, MainActivity.class);
           // intent.putExtras();
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
