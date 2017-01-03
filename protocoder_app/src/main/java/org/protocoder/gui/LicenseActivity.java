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

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.protocoder.R;
import org.protocoderrunner.base.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class LicenseActivity extends BaseActivity {

    private static final java.lang.String TAG = LicenseActivity.class.getSimpleName();
    private AssetManager mAssetManager;
    private String[] mLicenseFiles;
    private ArrayList<License> mLicenseFileContent = new ArrayList<>();

    private ListView mLicenseList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license_activity);
        mLicenseList = (ListView) findViewById(R.id.license_list);

        final MyAdapter myAdapter = new MyAdapter(this, mLicenseFileContent);
        mLicenseList.setAdapter(myAdapter);

        setupActivity();

        mAssetManager = getAssets();

        final Handler handler = new Handler();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                // read mCurrentFileList
                try {
                     mLicenseFiles = mAssetManager.list("licenses");
                    for (int i = 0; i < mLicenseFiles.length; i++) {
                        mLicenseFileContent.add(new License(mLicenseFiles[i], readFile("licenses/" + mLicenseFiles[i])));
                        // MLog.d(TAG, filecontent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // show license in ui
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mLicenseFiles.length; i++) {
                            View v = getLayoutInflater().inflate(R.layout.license_view, null);
                            TextView txtView = (TextView) v.findViewById(R.id.license_title);
                            txtView.setText(mLicenseFiles[i]);

                            myAdapter.notifyDataSetChanged();
                            mLicenseList.invalidateViews();
                        }
                    }
                });
            }
        });
        t.start();

    }

    @Override
    protected void setupActivity() {
        super.setupActivity();

        enableBackOnToolbar();
    }

    private String readFile(String path) throws IOException {
        InputStream is = mAssetManager.open(path);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = is.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = is.read();
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }

    class License {
        public String title;
        public String body;
        public boolean showing = false;

        public License(String name, String content) {
            title = name.replace("_", " ").replace(".txt", "");
            body = content;
        }
    }

    private class MyAdapter extends ArrayAdapter<License> {

        public MyAdapter(Context context, ArrayList<License> strings) {
            super(context, -1, -1, strings);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.license_view, parent, false);
            }
            TextView txtTitle = (TextView) convertView.findViewById(R.id.license_title);
            final TextView txtText = (TextView) convertView.findViewById(R.id.license_body);

            final License license = getItem(position);
            txtTitle.setText(license.title);
            txtText.setText(license.body);

            txtText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!license.showing) {
                        txtText.setEllipsize(null);
                        txtText.setMaxLines(Integer.MAX_VALUE);
                    }
                    else {
                        txtText.setEllipsize(TextUtils.TruncateAt.END);
                        txtText.setMaxLines(3);
                    }

                    license.showing = !license.showing;
                }
            });

            return convertView;
        }
    }

}
