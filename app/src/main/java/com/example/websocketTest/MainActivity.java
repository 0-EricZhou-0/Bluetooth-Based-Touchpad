package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    /**
     * Resources used when trying to connect.
     */
    // private static final int REQUEST_ENABLE_BT = 1;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button startConnection = findViewById(R.id.tryConnect);
        final Button settings = findViewById(R.id.settingInMainScreen);
        final Button exit = findViewById(R.id.exit);
        final TextView absorbMotionEvent = findViewById(R.id.absorbMotionEvent);

        try {
            Controls.init(MainActivity.this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        absorbMotionEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        absorbMotionEvent.setElevation(-1);

        View.OnClickListener connectToPC = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermanentConnection.connect();
            }
        };
        startConnection.setOnClickListener(connectToPC);

        View.OnClickListener openSettings = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ActivitySettings.class);
                startActivity(intent);
            }
        };
        settings.setOnClickListener(openSettings);

        View.OnClickListener exitApplication = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Warning")
                        .setMessage("Do you want to exit the application?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                finish();
                            }
                        }).show();
            }
        };
        exit.setOnClickListener(exitApplication);

        PermanentConnection.init(null, MainActivity.this, startConnection, absorbMotionEvent);
    }
}
