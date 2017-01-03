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

package org.protocoderrunner.api.network;

import android.os.Handler;
import android.os.Looper;

import org.protocoderrunner.api.common.ReturnInterfaceWithReturn;
import org.protocoderrunner.api.common.ReturnObject;
import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.models.Project;
import org.protocoderrunner.base.network.NetworkUtils;
import org.protocoderrunner.base.utils.MLog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class PSimpleHttpServer extends NanoHTTPD {
    public static final String TAG = PSimpleHttpServer.class.getSimpleName();
    public Handler mHandler = new Handler(Looper.getMainLooper());

    private AppRunner mAppRunner;

    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {
        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
        }
    };
    private ReturnInterfaceWithReturn mCallback = null;
    private Project mProject = null;

    public PSimpleHttpServer(AppRunner appRunner, int port) throws IOException {
        super(port);

        mAppRunner = appRunner;
        mProject = mAppRunner.getProject();
        String ip = NetworkUtils.getLocalIpAddress(mAppRunner.getAppContext()).get("ip");
        MLog.d(TAG, "Launched server at http://" + ip.toString() + ":" + port);

        appRunner.whatIsRunning.add(this);
    }

    public void start() {
        try {
            super.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onNewRequest(ReturnInterfaceWithReturn callbackfn) {
        MLog.d(TAG, "1 onNewRequest callback added " + callbackfn);
        this.mCallback = callbackfn;
    }

    @ProtoMethod(description = "Responds to the request with a given text", example = "")
    @ProtoMethodParam(params = {"boolean"})
    public Response response(String data) {
        Response r = newFixedLengthResponse(data);
        MLog.d(TAG, "responding with " + r);

        return r;
    }

    @ProtoMethod(description = "Responds to the request with a given text", example = "")
    @ProtoMethodParam(params = {"boolean"})
    public Response response(int code, String type, String data) {
        Status status = Status.lookup(code);
        return newFixedLengthResponse(status, MIME_TYPES.get(type), data);
    }

    public Response serveFile(String fileName) {
        Response res = null;

        String mime = getMimeType(fileName); // Get MIME type
        InputStream fi = null;
        try {
            String filePath = mProject.getFullPathForFile(fileName);
            MLog.d(TAG, "reading file " + filePath);
            fi = new FileInputStream(filePath);
            res = newFixedLengthResponse(Response.Status.OK, mime, fi, fi.available());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }


    String getMimeType(String uri) {
        String mime = null;
        int dot = uri.lastIndexOf('.');
        if (dot >= 0) mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        if (mime == null) mime = MIME_TYPES.get("binary");

        return mime;
    }
    @ProtoMethod(description = "Serves a file", example = "")
    @ProtoMethodParam(params = {"uri", "header"})
    @Override
    public Response serve(IHTTPSession session) {
        if (mCallback == null) return null;

        ReturnObject ret = new ReturnObject();
        ret.put("uri", session.getUri().toString());
        ret.put("method", session.getMethod().toString());
        ret.put("header", session.getHeaders());
        ret.put("params", session.getParameters());
        // ret.put("files", session.get());
        MLog.d(TAG, "2 calling callback");
        Response res = (Response) mCallback.event(ret);

        if (res == null) MLog.d(TAG, "2 is null");
        MLog.d(TAG, "response: " + res);

        if (res == null) {
            try {
                // file upload
                /*
                if (!files.isEmpty()) {
                    File src = new File(files.getProperty("pic").toString());
                    File dst = new File(mProjectFolder + parms.getProperty("pic").toString());

                    FileIO.copyFile(src, dst);

                    JSONObject data = new JSONObject();
                    data.put("result", "OK");

                    return new Response("200", MIME_TYPES.get("txt"), data.toString());

                    // normal file serving
                } else {

                    MLog.d(TAG, "received String" + uri + " " + method + " " + header + " " + " " + parms + " " + files);

                    res[0] = serveFile(uri.substring(uri.lastIndexOf('/') + 1, uri.length()), header,
                            new File(mProjectFolder), false);

                }
                */

            } catch (Exception e) {
                MLog.d(TAG, "response error " + e.toString());
            }

        }

        //MLog.d(TAG, "im returning " + res[0] + " " + res[0].status + " " + res[0].data);

        return res;
    }

    @ProtoMethod(description = "Stops the http server", example = "")
    @ProtoMethodParam(params = {""})
    public void stop() {
        super.stop();
    }

}
