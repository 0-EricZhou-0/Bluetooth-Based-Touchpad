package com.example.websocketTest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends AppCompatActivity {


    private AlertDialog.Builder exitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Controls.init(MainActivity.this);
        } catch (Exception e) {
            Log.println(Log.ERROR, "MainActivity", e.getMessage());
        }
        setContentView(R.layout.activity_main);

        final Button startConnection = findViewById(R.id.tryConnect);
        final Button settings = findViewById(R.id.settingInMainScreen);
        final Button exit = findViewById(R.id.exit);
        final ConstraintLayout mainWindow = findViewById(R.id.mainWindow);

        exitDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.warning)
                .setMessage(R.string.exitApplicationConfirm)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                        try {
                            System.exit(0);
                        } catch (Exception ignore) {
                        }
                    }
                });

        startConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BluetoothConnection.hasServerMac()) {
                    BluetoothConnection.connect();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.macRequired)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Controls.SettingDetail.setCurrentSettingTab(0);
                                    Intent intent = new Intent(MainActivity.this, ActivitySettings.class);
                                    startActivity(intent);
                                }
                            }).show();
                }
            }
        });

        View.OnClickListener openSettings = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ActivitySettings.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        };
        settings.setOnClickListener(openSettings);

        View.OnClickListener exitApplication = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDialog.show();
            }
        };
        exit.setOnClickListener(exitApplication);

        BluetoothConnection.init(MainActivity.this, MainActivity.this, startConnection, mainWindow);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, Test.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        exitDialog.show();
    }
}
