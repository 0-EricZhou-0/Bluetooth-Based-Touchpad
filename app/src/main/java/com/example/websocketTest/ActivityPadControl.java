package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityPadControl extends AppCompatActivity {

    private int lastXCoord;
    private int lastYCoord;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView touchPad = findViewById(R.id.touchPad);
        /*
        touchPad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int xCoordChange = (int) event.getRawX() - lastXCoord;
                int yCoordChange = (int) event.getRawY() - lastYCoord;
                lastXCoord = (int) event.getRawX();
                lastYCoord = (int) event.getRawY();
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    MainActivity.pc.sendCoord(xCoordChange, yCoordChange);
                }
                return true;
            }
        });

         */
    }
}
