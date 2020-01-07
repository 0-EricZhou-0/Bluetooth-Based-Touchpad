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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PermanentConnection {
    private static final String TAG = "PermanentConnection";
    private static final String separator = Character.toString((char) 9);

    /**
     * Try connect using multithreading.
     */
    private static class TryConnect extends Thread {
        boolean isConnecting = true;
        ProgressBar progressBar = activity.findViewById(R.id.connectionProgress);
        final int progressMax = progressBar.getMax();

        private void completeProgressLoading(final ProgressBar progressBar) {
            final int progressNow = progressBar.getProgress();
            final int progressMax = progressBar.getMax();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        try {
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
                    int eachProgress = 0;

                    @Override
                    public void run() {
                        while (isConnecting && progressNow + eachProgress < progressMax) {
                            try {
                                sleep(3);
                                eachProgress = (int) Math.exp(Math.log((progressMax - progressNow) / 300));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            progressBar.setProgress(progressNow += eachProgress);
                        }
                    }
                }).start();
                Log.i(TAG, "Start connection");
                if (serverMac.equals("00:00:00:00:00:00")) {
                    Log.i(TAG, "Dummy connection started");
                    inReader = new BufferedReader(new InputStreamReader(new DataInputStream(System.in), StandardCharsets.UTF_8));
                    outWriter = new PrintWriter(new OutputStreamWriter(new DataOutputStream(System.out), StandardCharsets.UTF_8), true);
                } else {
                    BluetoothDevice device = btAdapter.getRemoteDevice(serverMac);
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    btAdapter.cancelDiscovery();
                    btSocket.connect();
                    Log.i(TAG, "Connecting");
                    Thread.sleep(300);
                    InputStream inStream = btSocket.getInputStream();
                    inReader = new BufferedReader(new InputStreamReader(new DataInputStream(inStream), StandardCharsets.UTF_8));
                    OutputStream outStream = btSocket.getOutputStream();
                    outWriter = new PrintWriter(new OutputStreamWriter(new DataOutputStream(outStream), StandardCharsets.UTF_8), true);
                    Log.i(TAG, "inReader: " + inReader);
                    Thread.sleep(200);
                    if (inReader.ready()) {
                        String lineIn = inReader.readLine();
                        if (!lineIn.equals("CONNECTED")) {
                            throw new IllegalArgumentException("MESSAGE ERROR");
                        }
                        Log.i(TAG, "Connected");
                    } else {
                        throw new Exception("Overtime");
                    }
                }

                isConnecting = false;
                completeProgressLoading(progressBar);
                Intent intent = new Intent(context, ActivityConnected.class);
                context.startActivity(intent);
                ((Activity) context).finish();
            } catch (Exception ex1) {
                Log.i(TAG, "Connection not established");
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

        private static void identifyAndSend(byte innerAction, Object[] parameters) {
            Controls.TaskDetail detail = mapping.get(innerAction, Controls.ACTION_NOT_FOUND);
            if (detail != null) {
                int outerAction = detail.getTask();
                if (outerAction != Controls.ACTION_NOT_FOUND.getTask()) {
                    if (!detail.getCanBeRepeated()) {
                        return;
                    }
                    if (!Controls.TaskDetail.correspondsTo(outerAction).getCanBeRepeated() && detail.getCanBeRepeated()) {
                        detail.setCanBeRepeated(false);
                    }
                    StringBuilder toSend = new StringBuilder(Integer.toString(outerAction));
                    if (parameters != null) {
                        for (Object param : parameters) {
                            toSend.append(separator).append(param);
                        }
                    }
                    PermanentConnection.sendMessage(toSend.toString());
                    ActivityConnected.noInteractionTime = 0;
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
        outWriter.println(toSend);
        Log.i(TAG, "Message sent: " + toSend);
    }

    static void identifyAndSend(byte info, Object... parameters) {
        TouchEventMappingControl.identifyAndSend(info, parameters);
    }

    public static String receiveMessage() throws IOException {
        return inReader.readLine();
    }
}
