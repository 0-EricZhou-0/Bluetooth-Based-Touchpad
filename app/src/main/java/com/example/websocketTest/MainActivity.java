package com.example.websocketTest;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    /**
     * Resources used when trying to connect.
     */
    private static final int REQUEST_ENABLE_BT = 1;
    PermanentConnection pc = null;

    private TextView out;


    public class PermanentConnection {
        /**
         * Try connect using multithreading.
         */
        private class TryConnect extends Thread {
            public void run() {
                try {
                    BluetoothDevice device = btAdapter.getRemoteDevice(serverMac);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("ADDRESS")
                            .setMessage(device.getAddress() + "\n" + device.getName())
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                }
                            }).show();
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    btSocket.connect();
                    Toast.makeText(MainActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
                    inStream = btSocket.getInputStream();
                    inReader = new BufferedReader(new InputStreamReader(inStream));
                    outStream = btSocket.getOutputStream();
                    outWriter = new PrintWriter(new OutputStreamWriter(outStream));
                    Toast.makeText(MainActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
                    sleep(500);
                    Toast.makeText(MainActivity.this, btSocket.toString(), Toast.LENGTH_LONG).show();
                    btAdapter.cancelDiscovery();
                } catch (Exception e) {
                    AlertBox("Fatal Error", "Socket create failed:\n" + e.getMessage());
                    try {
                        btSocket.close();
                    } catch (IOException ioException) {
                        AlertBox("Fatal Error", "Cannot close socket:\n" + e.getMessage());
                    }
                }
            }
        }
        private final UUID MY_UUID =
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Well known SPP UUID
        private String serverMac = "5C:E0:C5:5B:28:AD";
        private BluetoothAdapter btAdapter = null;
        private BluetoothSocket btSocket = null;
        private InputStream inStream;
        private OutputStream outStream;
        BufferedReader inReader;
        PrintWriter outWriter;


        /**
         * If bluetooth is not turned on, tell the user to turn it on.
         * @param setMac    mac address of remote device
         */
        PermanentConnection(String setMac) {
            if (!(setMac == null || setMac.equals(""))) {
                serverMac = setMac;
            }
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter == null) {
                AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
            } else {
                if (btAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    //Prompt user to turn on Bluetooth
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Toast.makeText(MainActivity.this, "Enabled", Toast.LENGTH_SHORT).show();
                }
            }
        }
        public void connect() {
            TryConnect tryConnect = new TryConnect();
            tryConnect.start();
        }
        public void sendMessage(Long toSend) {
            if (outWriter == null) {
                Toast.makeText(MainActivity.this, "Out not Connected", Toast.LENGTH_SHORT).show();
                return;
            }
            outWriter.print(toSend);
            outWriter.flush();
            outWriter.print("\n");
            outWriter.flush();
        }
        public void receiveMessage() {
            if (inReader == null) {
                Toast.makeText(MainActivity.this, "In not Connected", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String lineRead = inReader.readLine();
                Toast.makeText(MainActivity.this, lineRead, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                AlertBox("Fatal Error", "Reading error");
            }
        }
        public BluetoothAdapter getBluetoothAdapter() {
            return btAdapter;
        }
        public BluetoothSocket getBluetoothSocket() {
            return btSocket;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        out = findViewById(R.id.textSend);
        Button request = findViewById(R.id.tryConnect);
        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pc = new PermanentConnection(null);
            }
        });

        Button establish = findViewById(R.id.establishSocket);
        establish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pc.connect();
            }
        });

        Button send = findViewById(R.id.trySend);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pc.sendMessage(System.currentTimeMillis());
            }
        });

        Button receive = findViewById(R.id.tryReceieve);
        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pc.receiveMessage();
            }
        });
    }

    public void AlertBox(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message + " \nPress OK to exit.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }

}