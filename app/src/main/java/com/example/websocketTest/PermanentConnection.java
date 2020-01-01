package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

public class PermanentConnection {
    private static final String TAG = "PermanentConnection";

    /**
     * Try connect using multithreading.
     */
    private static class TryConnect extends Thread {
        boolean isConnecting = true;
        ProgressBar progressBar = activity.findViewById(R.id.connectionProgress);
        final int progressMax = progressBar.getMax();
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
            try {
                new Thread(new Runnable() {
                    int progressNow = progressBar.getMin();
                    int eachProgress = (int) Math.exp(Math.log((progressMax - progressNow) / 500));
                    @Override
                    public void run() {
                        while (isConnecting && progressNow + eachProgress < progressMax) {
                            try {
                                sleep(3);
                                eachProgress = (int) Math.exp(Math.log((progressMax - progressNow) / 500));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            progressBar.setProgress(progressNow += eachProgress);
                        }
                    }
                }).start();
                Log.i(TAG, "Start connection");
                BluetoothDevice device = btAdapter.getRemoteDevice(serverMac);
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btAdapter.cancelDiscovery();
                btSocket.connect();
                Log.i(TAG, "Connecting");
                Thread.sleep(1000);
                InputStream inStream = btSocket.getInputStream();
                inReader = new BufferedReader(new InputStreamReader(inStream));
                OutputStream outStream = btSocket.getOutputStream();
                outWriter = new PrintWriter(new OutputStreamWriter(outStream));
                Log.i(TAG, "inReader: " + inReader);
                String lineIn = inReader.readLine();
                if (!lineIn.equals("CONNECTED")) {
                    throw new IllegalArgumentException("MESSAGE ERROR");
                }
                Log.i(TAG, "Connected");

                isConnecting = false;
                completeProgressLoading(progressBar);
                Intent intent = new Intent(context, ActivityConnected.class);
                context.startActivity(intent);
                ((Activity) context).finish();

            } catch (Exception ex1) {
                Log.i(TAG, "Exception not connected");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Controls.setUsability(true, rootView);
                        completeProgressLoading(progressBar);
                        startConnection.setText(R.string.failToEstablishConnection);
                    }
                });
                isConnecting = false;
                try {
                    btSocket.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static void completeProgressLoading(final ProgressBar progressBar) {
        final int progressNow = progressBar.getProgress();
        final int progressMax = progressBar.getMax();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    try{
                        Thread.sleep(2);
                        progressBar.setProgress((progressMax - progressNow) / 100 * i + progressNow);
                    } catch (InterruptedException e) {
                        progressBar.setProgress(progressMax);
                    }
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
                progressBar.setProgress(progressBar.getMin());
            }
        }).start();
    }

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Well known SPP UUID
    private static String serverMac;
    private static BluetoothAdapter btAdapter = null;
    private static BluetoothSocket btSocket = null;
    private static BufferedReader inReader;
    private static PrintWriter outWriter;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @SuppressLint("StaticFieldLeak")
    private static Activity activity;
    @SuppressLint("StaticFieldLeak")
    private static Button startConnection;
    @SuppressLint("StaticFieldLeak")
    private static ViewGroup rootView;

    static void setServerMac(String mac) {
        serverMac = mac;
    }

    static boolean hasServerMac() {
        return serverMac != null;
    }

    static class TouchEventMappingControl {
        static SparseArray<Controls.TaskDetail> mapping = new SparseArray<>();

        static void updateMapping() {
            mapping = Controls.getCurrentMappingsDuplicated();
            for (int i = 0; i < mapping.size(); i++) {
                mapping.valueAt(i).setCanBeRepeated(true);
            }
        }

        static void identifyAndSend(byte innerAction, int[] parameters) {
            Controls.TaskDetail detail = mapping.get(innerAction);
            if (detail != null) {
                String outerAction = detail.getTask();
                // Toast.makeText(context, innerAction + outerAction, Toast.LENGTH_LONG).show();
                if (!outerAction.equals(Controls.ACTION_NOT_FOUND.getTask())) {
                    if (!detail.getCanBeRepeated()) {
                        return;
                    }
                    if (!Controls.TaskDetail.correspondsTo(outerAction).getCanBeRepeated() && detail.getCanBeRepeated()) {
                        detail.setCanBeRepeated(false);
                    }
                    StringBuilder toSend = new StringBuilder(outerAction);
                    if (parameters != null) {
                        for (int param : parameters) {
                            toSend.append(" ").append(param);
                        }
                    }
                    PermanentConnection.sendMessage(toSend.toString());
                }
                if (innerAction == Controls.MOVE_CANCEL) {
                    for (int i = 0; i < mapping.size(); i++) {
                        mapping.valueAt(i).setCanBeRepeated(true);
                    }
                }
            }
        }
    }

    static void init(Context setContext, Activity setActivity, Button tryConnect, ViewGroup root) {
        startConnection = tryConnect;
        rootView = root;
        context = setContext;
        activity = setActivity;
        TouchEventMappingControl.updateMapping();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                btAdapter.enable();
                throw new RuntimeException("Bluetooth adapter not enabled");
            }
        }
    }

    static void connect() {
        TryConnect tryConnect = new TryConnect();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startConnection.setText(String.format("%s...", context.getString(R.string.tryingToConnect)));
                Controls.setUsability(false, rootView);
            }
        });
        tryConnect.start();
    }

    static void disconnect() {
        try {
            btSocket.close();
        } catch (IOException ignore) {
        }
    }

    static void sendMessage(String toSend) {
        outWriter.print(toSend + "\n");
        outWriter.flush();
    }

    static void identifyAndSend(byte info, int... parameters) {
        TouchEventMappingControl.identifyAndSend(info, parameters);
        ActivityConnected.noInteractionTime = 0;
    }

    public static String receiveMessage() throws IOException {
        return inReader.readLine();
    }
}
