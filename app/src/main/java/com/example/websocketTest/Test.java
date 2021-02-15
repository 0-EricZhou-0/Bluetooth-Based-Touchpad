package com.example.websocketTest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class Test extends Activity implements OnClickListener {

    public static final String TAG = "USBCommActivity";
    public static final int TIMEOUT = 10;
    private Button btnStartServer;
    private String connectionStatus = null;
    private Handler mHandler = null;
    private ServerSocket server = null;
    private Socket client = null;
    private ObjectOutputStream out;
    private BufferedInputStream in;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        btnStartServer = findViewById(R.id.btnView);
        btnStartServer.setOnClickListener(this);
        mHandler = new Handler();
    }

    /**
     * Thread to initialize Socket connection
     */
    private final Runnable initializeConnection = new Thread() {
        @Override
        public void run() {
            // initialize server socket
            try {
                final ImageView imgView = findViewById(R.id.imageView1);
                server = new ServerSocket(38300);
                server.setSoTimeout(Test.TIMEOUT * 3000);
                //attempt to accept a connection
                client = server.accept();

                out = new ObjectOutputStream(client.getOutputStream());
                in = new BufferedInputStream(client.getInputStream());

                Log.i(TAG, "CONNECTED");
                byte[] arr = new byte[2000000];
                ByteArrayInputStream in2 = new ByteArrayInputStream(arr);
                /*
                for (int i = 0; i < 10000; i++) {
                    Log.i(TAG, "" + in.read(arr));
                }
                Log.i(TAG, new String(arr));
                 */
                int idx = 0;
                int total = 0;
                final long start;
                in.read();
                start = System.currentTimeMillis();
                try {
                    while (true) {
                        int length = in.read(arr);
                        total +=length;
                        Log.i(TAG, "" + length);
                        final Bitmap img = BitmapFactory.decodeByteArray(arr, 0, length);
                        // imgView.setImageBitmap(img);
                        if (img == null) {
                            Log.i(TAG, "null");
                        } else {
                            Log.i(TAG, img + "");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imgView.setImageBitmap(img);
                                }
                            });
                        }
                        idx++;
                        Log.i(TAG, "" + idx);
                    }
                } catch (Exception e) {
                    long time = (System.currentTimeMillis() - start);
                    Log.i(TAG, "Time:    " + time);
                    Log.i(TAG, "Total:   " + total);
                    Log.i(TAG, "Average: " + (total / 1024.0 / 1024 / time * 1000));
                }
            } catch (SocketTimeoutException e) {
                connectionStatus = "Connection has timed out! Please try again";
                mHandler.post(showConnectionStatus);
                try {
                    server.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                Log.e(Test.TAG, "" + e);
                e.printStackTrace();
            }

            if (client != null) {
                connectionStatus = "Connection was successful!";
                mHandler.post(showConnectionStatus);
            }
        }
    };

    /**
     * Runnable to show pop-up for connection status
     */
    private final Runnable showConnectionStatus = new Runnable() {
        //----------------------------------------

        /**
         * @see java.lang.Runnable#run()
         */
        //----------------------------------------
        @Override
        public void run() {
            Toast.makeText(Test.this, connectionStatus, Toast.LENGTH_SHORT).show();
        }
    };

    //----------------------------------------

    /**
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    //----------------------------------------
    @Override
    public void onClick(View v) {
        //initialize server socket in a new separate thread
        new Thread(initializeConnection).start();
        String msg = "Attempting to connect¦";
        Toast.makeText(Test.this, msg, msg.length()).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            // Close the opened resources on activity destroy
            in.close();
            out.close();
            if (server != null) {
                server.close();
            }
        } catch (IOException ec) {
            Log.e(Test.TAG, "Cannot close server socket" + ec);
        }
    }
}
