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

package org.protocoder.helpers;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protocoder.settings.ProtocoderSettings;
import org.protocoderrunner.base.utils.FileIO;
import org.protocoderrunner.base.utils.MLog;
import org.protocoderrunner.models.Folder;
import org.protocoderrunner.models.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ProtoScriptHelper {

    private static final String TAG = ProtoScriptHelper.class.getSimpleName();

    private static String getBaseDir() {
        String baseDir;

        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + ProtocoderSettings.PROTOCODER_FOLDER + File.separator;

        return baseDir;
    }

    public static String getBackupFolderPath() {
        return getProjectFolderPath("backup");
    }

    //
    public static String getProjectFolderPath(String folder) {
        return getBaseDir() + folder;
    }

    // Check if a folder project exists
    public static boolean isProjectExisting(String folder, String name) {
        ArrayList<Project> projects = ProtoScriptHelper.listProjects(folder, false);

        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    // Create Project
    public static Project createNewProject(Context c, String newProjectName, String folder, String fileName) {
        String newTemplateCode = FileIO.readAssetFile(c, "templates/new.js");

        if (newTemplateCode == null) {
            newTemplateCode = "";
        }
        FileIO.writeStringToFile(getProjectFolderPath(folder), newProjectName, newTemplateCode);
        Project newProject = new Project(folder, newProjectName);

        return newProject;
    }

    // Delete Project
    public static void deleteFolder(String path) {
        File dir = new File(path);

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                new File(dir, element).delete();
            }
        }
        dir.delete();
    }

    // Write a file with code
    public static void writeCode(String code, String filename) {

        File f = new File(filename);

        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(f);
            byte[] data = code.getBytes();
            fo.write(data);
            fo.flush();
            fo.close();

        } catch (FileNotFoundException ex) {
            MLog.e(TAG, ex.toString());
        } catch (IOException e) {
            e.printStackTrace();
            // Log.e("Project", e.toString());
        }
    }

    // Get code from sdcard
    public static String getCode(Project p) {
        String path = p.getFullPath() + File.separator + ProtocoderSettings.MAIN_FILENAME;

        return FileIO.loadCodeFromFile(path);
    }

    // List folders
    public static ArrayList<Folder> listFolders(String folder, boolean orderByName) {
        ArrayList<Folder> folders = new ArrayList<Folder>();
        File dir = new File(ProtocoderSettings.getFolderPath(folder));

        if (!dir.exists()) {
            dir.mkdir();
        }

        File[] all_projects = dir.listFiles();

        if (orderByName) {
            Arrays.sort(all_projects);
        }

        for (File file : all_projects) {
            String projectURL = file.getAbsolutePath();
            String projectName = file.getName();
            folders.add(new Folder(folder, projectName));
        }

        return folders;
    }

    // List projects
    public static ArrayList<Project> listProjects(String folder, boolean orderByName) {
        ArrayList<Project> projects = new ArrayList<Project>();
        File dir = new File(ProtocoderSettings.getFolderPath(folder));

        if (!dir.exists()) {
            dir.mkdir();
        }

        File[] all_projects = dir.listFiles();

        if (orderByName) {
            Arrays.sort(all_projects);
        }

        for (File file : all_projects) {
            String projectURL = file.getAbsolutePath();
            String projectName = file.getName();

            projects.add(new Project(folder, projectName));
        }

        return projects;
    }


    public static String exportProjectAsProtoFile(Project p) {

        // TODO: Use thread

        String givenName = getBackupFolderPath() + File.separator + p.getPath() + "_" + p.getName();

        //check if file exists and rename it if so
        File f = new File(givenName + ProtocoderSettings.PROTO_FILE_EXTENSION);
        int num = 1;
        while (f.exists()) {
            f = new File(givenName + "_" + num++ + ProtocoderSettings.PROTO_FILE_EXTENSION);
        }

        //compress
        try {
            FileIO.zipFolder(p.getFullPath(), f.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //return the filepath of the backup
        return f.getAbsolutePath();
    }


    public static boolean importProtoFile(String folder, String zipFilePath) {

        // TODO: Use thread

        //decompress
        try {
            FileIO.extractZip(zipFilePath, getProjectFolderPath(folder));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }




    // Get code from assets
    // public static String getCodeFromAssets(Context c, Project p) {
    //    return FileIO.readAssetFile(c, getProjectPath() + File.separator + ProtocoderSettings.PROTO_FILE_EXTENSION);
    // }




    public static JSONObject toJson(Project p) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", p.getName());
            json.put("folder", p.getPath());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


    public static ArrayList<File> listFilesInProject(Project p) {
        ArrayList<File> files = new ArrayList<File>();

        File f = new File(p.getFullPath());
        File file[] = f.listFiles();

        for (File element : file) {
            files.add(element);
        }

        return files;
    }

    public static JSONArray listFilesInProjectJSON(Project p) {

        File f = new File(p.getFullPath());
        File file[] = f.listFiles();
        MLog.d("Files", "Size: " + file.length);

        JSONArray array = new JSONArray();
        for (File element : file) {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("file_name", element.getName());
                jsonObject.put("file_size", element.length() / 1024);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            array.put(jsonObject);
            MLog.d("Files", "FileName:" + element.getName());
        }

        return array;
    }



}