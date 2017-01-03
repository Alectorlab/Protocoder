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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.mozilla.javascript.NativeArray;
import org.protocoderrunner.api.ProtoBase;
import org.protocoderrunner.api.common.ReturnInterface;
import org.protocoderrunner.api.common.ReturnObject;
import org.protocoderrunner.api.other.WhatIsRunningInterface;
import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.base.utils.MLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class PBluetoothClient extends ProtoBase implements WhatIsRunningInterface {

    private final PBluetooth mPBluetooth;
    private ReturnInterface mCallbackConnected;
    private ReturnInterface mCallbackData;

    private final static int DISCONNECTED = 0;
    private final static int CONNECTING = 1;
    private final static int CONNECTED = 2;

    private BluetoothDevice mDevice;

    private int status;


    public PBluetoothClient(PBluetooth pBluetooth, AppRunner appRunner) {
        super(appRunner);
        mPBluetooth = pBluetooth;
        status = DISCONNECTED;
    }

    public PBluetoothClient onConnected(ReturnInterface callbackOnConnected) {
        mCallbackConnected = callbackOnConnected;
        return this;
    }

    public void onNewData(ReturnInterface callbackfn) {
        mCallbackData = callbackfn;
    }

    @ProtoMethod(description = "Connects to a bluetooth device using a popup with the available devices", example = "")
    @ProtoMethodParam(params = {"function(name, macAddress, strength)"})
    public void connectSerial() {
        NativeArray nativeArray = mPBluetooth.getBondedDevices();
        String[] arrayStrings = new String[(int) nativeArray.size()];
        for (int i = 0; i < nativeArray.size(); i++) {
            arrayStrings[i] = (String) nativeArray.get(i, null);
        }

        getAppRunner().pUi.popup().title("Connect to device").choice(arrayStrings).onAction(new ReturnInterface() {
            @Override
            public void event(ReturnObject r) {
                String addr = (String) r.get("answer");
                MLog.d(TAG, "address " + addr);
                connectSerial(addr.split(" +")[1]);
            }
        }).show();
    }


    @ProtoMethod(description = "Connect to mContext bluetooth device using the mac address", example = "")
    @ProtoMethodParam(params = {"mac", "function(data)"})
    public void connectSerial(String mac) {
        BluetoothDevice device = mPBluetooth.getAdapter().getRemoteDevice(mac);
        tryToConnect(device);
    }

    @ProtoMethod(description = "Connect to mContext bluetooth device using mContext name", example = "")
    @ProtoMethodParam(params = {"name, function(data)"})
    public PBluetoothClient connectSerialByName(String name) {
        Set<BluetoothDevice> pairedDevices = mPBluetooth.getAdapter().getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(name)) {
                    tryToConnect(device);

                    break;
                }
            }
        }
        return this;
    }

    @ProtoMethod(description = "Send bluetooth serial message", example = "")
    @ProtoMethodParam(params = {"string"})
    public void send(String string) {
        mConnectedThread.write(string.getBytes());
    }

    @ProtoMethod(description = "Send bluetooth serial message", example = "")
    @ProtoMethodParam(params = {"int"})
    public void sendBytes(byte[] bytes) {
        mConnectedThread.write(bytes);
    }

    @ProtoMethod(description = "Disconnect the bluetooth", example = "")
    @ProtoMethodParam(params = {""})
    public void disconnect() {
        if (mConnectingThread != null) {
            mConnectingThread.cancel();
            mConnectingThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.shutdown();
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        changeStatus(DISCONNECTED, mDevice);
    }

    @ProtoMethod(description = "Enable/Disable the bluetooth adapter", example = "")
    @ProtoMethodParam(params = {"boolean"})
    public boolean isConnected() {
        return status == CONNECTED;
    }

    @Override
    public void __stop() {
        disconnect();
    }


    /***********************************************************************************
     * IMPL
     */


    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;



    // connection thread
    public synchronized void tryToConnect(BluetoothDevice device) {
        MLog.d(TAG, "connect to: " + device);

        // if trying to connect cancel any thread
        if (status == CONNECTING) {
            if (mConnectingThread != null) {
                mConnectingThread.cancel();
                mConnectingThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        try {
            mConnectingThread = new ConnectingThread(device);
            mConnectingThread.start();
            changeStatus(CONNECTING, device);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) throws SecurityException, NoSuchMethodException,
                IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            mmDevice = device;
            BluetoothSocket tmpSocket = null;

            // Get mContext BluetoothSocket for mContext connection with the given BluetoothDevice
            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(mPBluetooth.UUID_SPP);
                MLog.d(TAG, "socketTmp " + tmpSocket);
            } catch (IOException e) {
                Log.e(TAG, "create socket failed, trying with new fallback", e);

                Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                tmpSocket = (BluetoothSocket) m.invoke(device, 1);
                MLog.d(TAG, "socketTmp 2" + tmpSocket);
            }


            mmSocket = tmpSocket;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mPBluetooth.getAdapter().cancelDiscovery();

            // Make mContext connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();

                changeStatus(DISCONNECTED, mmDevice);

                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectingThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    // start connection thread
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        MLog.d(TAG, "connected");

        changeStatus(CONNECTED, device);
        mDevice = device;

        // Cancel the thread that completed the connection
        if (mConnectingThread != null) {
            mConnectingThread.cancel();
            mConnectingThread = null;
        }

        // Cancel any thread currently running mContext connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, device);
        mConnectedThread.start();
    }




    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothDevice mmDevice;

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            MLog.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            mmDevice = device;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        private boolean stop = false;
        private final boolean hasReadAnything = false;

        public void shutdown() {
            stop = true;
            if (!hasReadAnything) {
                return;
            }
            if (mmInStream != null) {
                try {
                    mmInStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of InputStream failed.");
                }
            }
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            BufferedReader reader = new BufferedReader(new InputStreamReader(mmInStream));

            while (!stop) {
                try {
                    final String line = reader.readLine();
                    if (line != null) {
                        MLog.d(TAG, line);

                        if (mCallbackData != null) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //MLog.d(TAG, "Got data: " + data);
                                    ReturnObject o = new ReturnObject();
                                    o.put("data", line);
                                    mCallbackData.event(o);
                                }
                            });
                        }

                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    changeStatus(DISCONNECTED, mmDevice);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void changeStatus (int status, BluetoothDevice device) {
        if (mCallbackConnected != null) {

            String returnString = "";

            switch (status) {
                case DISCONNECTED:
                    returnString = "disconnected";
                    break;

                case CONNECTING:
                    returnString = "connecting";
                    break;

                case CONNECTED:
                    returnString = "connected";
                    break;

            }

            ReturnObject o = new ReturnObject();
            o.put("status", returnString);

            if (device != null) {
                o.put("mac", device.getAddress());
                o.put("name", device.getName());
            }
            mCallbackConnected.event(o);
        }
    }


}
