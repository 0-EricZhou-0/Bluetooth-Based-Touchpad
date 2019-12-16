package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

public class PermanentConnection {

    /**
     * Try connect using multithreading.
     */
    private static class TryConnect extends Thread {
        @SuppressLint("SetTextI18n")
        public void run() {
            try {
                startConnection.setText("Trying to connect...");
                absorb.setElevation(100);
                BluetoothDevice device = btAdapter.getRemoteDevice(serverMac);
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btAdapter.cancelDiscovery();
                btSocket.connect();
                Thread.sleep(1000);
                InputStream inStream = btSocket.getInputStream();
                inReader = new BufferedReader(new InputStreamReader(inStream));
                OutputStream outStream = btSocket.getOutputStream();
                outWriter = new PrintWriter(new OutputStreamWriter(outStream));

                String lineIn = inReader.readLine();
                if (!lineIn.equals("CONNECTED")) {
                    throw new IllegalArgumentException("MESSAGE ERROR");
                }
                // heartbeat = new Heartbeat(3000, 3);
                // heartbeat.start();
                Intent intent = new Intent(context, ActivityConnected.class);
                context.startActivity(intent);
                ((Activity) context).finish();

            } catch (Exception ex1) {
                startConnection.setText("Connection failed, please try again");
                absorb.setElevation(-1);
                try {
                    btSocket.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Well known SPP UUID
    private static String serverMac = "5C:E0:C5:5B:28:AD";
    private static BluetoothAdapter btAdapter = null;
    private static BluetoothSocket btSocket = null;
    private static BufferedReader inReader;
    private static PrintWriter outWriter;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @SuppressLint("StaticFieldLeak")
    private static Button startConnection;
    @SuppressLint("StaticFieldLeak")
    private static TextView absorb;

    static class TouchEventMappingControl {
        static SparseArray<Controls.TaskDetail> mapping = new SparseArray<>();

        static void updateMapping() {
            mapping = Controls.getCurrentMapping();
            for (int i = 0; i < mapping.size(); i++) {
                mapping.valueAt(i).setCanBeRepeated(true);
            }
        }

        static void identifyAndSend(byte innerAction, int[] parameters) {
            Controls.TaskDetail detail = mapping.get(innerAction);
            if (detail != null) {
                String outerAction = detail.getOuterControl();
                // Toast.makeText(context, innerAction + outerAction, Toast.LENGTH_LONG).show();
                if (!outerAction.equals(Controls.ACTION_NOT_FOUND.getOuterControl())) {
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

    static void init(String setMac, Context setContext, Button tryConnect, TextView setAbsorb) {
        startConnection = tryConnect;
        absorb = setAbsorb;
        context = setContext;
        TouchEventMappingControl.updateMapping();
        if (!(setMac == null || setMac.equals(""))) {
            serverMac = setMac;
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                btAdapter.enable();
            }
        }
    }

    static void connect() {
        TryConnect tryConnect = new TryConnect();
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
