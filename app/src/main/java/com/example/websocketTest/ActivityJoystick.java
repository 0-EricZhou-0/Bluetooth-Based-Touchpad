package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityJoystick extends AppCompatActivity {
    private CoordinatePair startPos = Controls.NOT_STARTED;
    private CoordinatePair currentPos = Controls.NOT_STARTED;
    private String lastSend = "N";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);
        final TextView leftControl = findViewById(R.id.leftControl);
        final TextView rightControl = findViewById(R.id.rightControl);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        final View decorView = getWindow().getDecorView();
        Controls.maximumWindow(decorView);

        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                final Handler RightClickHandler = new Handler();
                RightClickHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Controls.maximumWindow(decorView);
                    }
                }, 1700);
            }
        });

        leftControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float currentX = event.getRawX();
                float currentY = event.getRawY();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startPos = new CoordinatePair(currentX, currentY);
                    currentPos = startPos;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    startPos = Controls.NOT_STARTED;
                    lastSend = "N";
                    BluetoothConnection.sendMessage("N");
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    currentPos = new CoordinatePair(currentX, currentY);
                    double angle = startPos.getPreciseDirection(currentPos);
                    if (startPos.getDistance(currentPos) < 100) {
                        leftControl.setText("N");
                        if (!lastSend.equals("N")) {
                            lastSend = "N";
                            BluetoothConnection.sendMessage("N");
                        }
                        return true;
                    }
                    String toSend;
                    if (angle >= Math.PI * 23 / 12 || angle < Math.PI / 12) {
                        toSend = "A";
                    } else if (angle >= Math.PI / 12 && angle < Math.PI / 4) {
                        toSend = "B";
                    } else if (angle >= Math.PI / 4 && angle < Math.PI * 5 / 12) {
                        toSend = "C";
                    } else if (angle >= Math.PI * 5 / 12 && angle < Math.PI * 7 / 12) {
                        toSend = "D";
                    } else if (angle >= Math.PI * 7 / 12 && angle < Math.PI * 3 / 4) {
                        toSend = "E";
                    } else if (angle >= Math.PI * 3 / 4 && angle < Math.PI * 11 / 12) {
                        toSend = "F";
                    } else if (angle >= Math.PI * 11 / 12 && angle < Math.PI * 13 / 12) {
                        toSend = "G";
                    } else if (angle >= Math.PI * 13 / 12 && angle < Math.PI * 5 / 4) {
                        toSend = "M";
                    } else if (angle >= Math.PI * 5 / 4 && angle < Math.PI * 17 / 12) {
                        toSend = "I";
                    } else if (angle >= Math.PI * 17 / 12 && angle < Math.PI * 19 / 12) {
                        toSend = "J";
                    } else if (angle >= Math.PI * 19 / 12 && angle < Math.PI * 7 / 4) {
                        toSend = "K";
                    } else { // if (angle >= Math.PI * 7 / 4 && angle < Math.PI * 23 / 12)
                        toSend = "L";
                    }
                    leftControl.setText(toSend);
                    if (!lastSend.equals(toSend)) {
                        BluetoothConnection.sendMessage(toSend);
                        lastSend = toSend;
                    }
                }
                return true;
            }
        });

        rightControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    BluetoothConnection.sendMessage("Q");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    BluetoothConnection.sendMessage("R");
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(ActivityJoystick.this)
                .setTitle("Warning")
                .setMessage("Do you want to exit touching pad?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        BluetoothConnection.sendMessage("X");
                        finish();
                    }
                }).show();
    }
}
