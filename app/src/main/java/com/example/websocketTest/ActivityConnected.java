package com.example.websocketTest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public class ActivityConnected extends AppCompatActivity {
    public static int noInteractionTime;
    private boolean exitFlag = false;
    private GestureDetectorCompat mDetector;
    Thread heartbeatThread;
    private AlertDialog.Builder exitDialog;

    class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            float xChange = event2.getRawX() - event1.getRawX();
            float yChange = event2.getRawY() - event1.getRawY();
            if (xChange - Math.abs(yChange) > 50) {
                exitDialog.show();
            }
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
        mDetector = new GestureDetectorCompat(this, new CustomGestureListener());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        heartbeatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (exitFlag) {
                        return;
                    }
                    noInteractionTime++;
                    if (noInteractionTime > 1) {
                        PermanentConnection.identifyAndSend(Controls.HEARTBEAT_ACTION);
                    }
                }
            }
        });
        heartbeatThread.start();

        exitDialog = new AlertDialog.Builder(ActivityConnected.this)
                .setTitle(R.string.warning)
                .setMessage("Disconnect?")
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        PermanentConnection.sendMessage("EXIT");
                        exitFlag = true;
                        Intent intent = new Intent(ActivityConnected.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

        final Button launchPad = findViewById(R.id.enableTouchPad);
        final Button launchGame = findViewById(R.id.enableJoystick);
        final ImageButton settings = findViewById(R.id.settings);

        launchPad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermanentConnection.sendMessage("TOUCH_PAD");
                Intent intent = new Intent(ActivityConnected.this, ActivityTouchPad.class);
                startActivity(intent);
            }
        });

        launchGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermanentConnection.sendMessage("GAME");
                Intent intent = new Intent(ActivityConnected.this, ActivityJoystick.class);
                startActivity(intent);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityConnected.this, ActivitySettings.class);
                intent.putExtra("changeDevicePermitted", false);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        exitDialog.show();
    }
}
