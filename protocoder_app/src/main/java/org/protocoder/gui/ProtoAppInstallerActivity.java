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

package org.protocoder.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.protocoder.R;
import org.protocoder.helpers.ProtoScriptHelper;
import org.protocoderrunner.base.BaseActivity;
import org.protocoderrunner.base.utils.MLog;
import org.protocoderrunner.models.Project;

import java.io.File;

public class ProtoAppInstallerActivity extends BaseActivity {

    private static final String TAG = ProtoAppInstallerActivity.class.getSimpleName();

    ProgressBar mProgress;
    Button mBtnFinish;
    private LinearLayout mLl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_proto_installer);

        TextView txtProto = (TextView) findViewById(R.id.text_proto_install_info);
        TextView txtOrigin = (TextView) findViewById(R.id.from_url);
        TextView txtDestiny = (TextView) findViewById(R.id.to_url);
        TextView txtWarning = (TextView) findViewById(R.id.text_proto_install_warning);

        mLl = (LinearLayout) findViewById(R.id.proto_install_group);
        Button btnInstall = (Button) findViewById(R.id.button_proto_install_ok);
        Button btnCancel = (Button) findViewById(R.id.button_proto_install_cancel);
        mBtnFinish = (Button) findViewById(R.id.button_proto_install_finish);
        mProgress = (ProgressBar) findViewById(R.id.progressBar_installing);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }

        final Uri urlData = intent.getData();
        txtProto.setText("A project is going to be installed");
        txtOrigin.setText("from: " + urlData.getPath());

        String folder = "user_projects/User Projects";
        File f = new File(urlData.getPath());

        int index = f.getName().lastIndexOf("_");
        String name = f.getName().substring(0, index); //replaceFirst("[.][^.]+$", "");
        final Project p = new Project(folder, name);
        txtDestiny.setText("to: " + p.getFullPath());

        //check if project is already installed
        if (p.exists()) {
            txtWarning.setVisibility(View.VISIBLE);
        }

        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                install(p, urlData);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //check if is autoinstall and proceed
        // Read in the script given in the intent.
        if (null != intent) {
            boolean autoInstall = intent.getBooleanExtra("autoInstall", false);

            if (autoInstall) {
                btnInstall.performClick();
                mBtnFinish.performClick();
            }
        }
    }

    private void install(Project p, Uri urlData) {
        mProgress.setVisibility(View.VISIBLE);

        if (urlData != null) {
            new File(p.getFullPath()).mkdirs();
            boolean ok = ProtoScriptHelper.installProject(this, urlData.getPath(), p.getFullPath());
            MLog.d(TAG, "installed " + ok);
            mProgress.setVisibility(View.GONE);

            mLl.setVisibility(View.GONE);
            mBtnFinish.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Proto app " + p.getName() + " installed", Toast.LENGTH_LONG).show();
        }
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