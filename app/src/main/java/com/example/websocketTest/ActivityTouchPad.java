package com.example.websocketTest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import static com.example.websocketTest.Controls.NOT_STARTED;
import static com.example.websocketTest.Controls.ZERO;

public class ActivityTouchPad extends AppCompatActivity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private final int MAX_NUM_OF_FINGERS_SUPPORTED = 5;
    private final int WAIT_UNTIL_CONFIRM = 160;
    private final int LONG_PRESS_LENGTH = 800;

    private GestureDetectorCompat mDetector;

    private FingerEvent[] eventGroup = new FingerEvent[MAX_NUM_OF_FINGERS_SUPPORTED];

    private int maxNumPointers = 1;
    private int currentNumPointers = 1;
    private int direction = -1;

    private boolean forwardScrollMode;
    private boolean touchWarningMode;
    private AlertDialog.Builder exitDialog;

    private class FingerEvent {
        CoordinatePair startPos = NOT_STARTED;
        CoordinatePair currentPos = NOT_STARTED;
        float maximumTolerance;

        FingerEvent(float setTolerance) {
            maximumTolerance = setTolerance;
        }

        void startEvent(float x, float y) {
            startPos = new CoordinatePair(x, y);
            currentPos = startPos;
        }

        void setCurrentPos(float x, float y) {
            currentPos = new CoordinatePair(x, y);
        }

        CoordinatePair moveTo(float x, float y) {
            CoordinatePair undeterminedCurrentPos = new CoordinatePair(x, y);
            float currentXChange = undeterminedCurrentPos.getXChange(currentPos);
            float currentYChange = undeterminedCurrentPos.getYChange(currentPos);
            int sendXChange = 0, sendYChange = 0;
            if (Math.abs(currentXChange) > maximumTolerance) {
                sendXChange = (int) (currentXChange / maximumTolerance);
                currentPos = undeterminedCurrentPos;
            }
            if (Math.abs(currentYChange) > maximumTolerance) {
                sendYChange = (int) (currentYChange / maximumTolerance);
                currentPos = undeterminedCurrentPos;
            }
            return new CoordinatePair(sendXChange, sendYChange);
        }

        void reset() {
            startPos = NOT_STARTED;
            currentPos = NOT_STARTED;
        }

        boolean getDistanceLessThan(int maximumDist) {
            return startPos.getDistance(currentPos) < maximumDist;
        }

        boolean hasNotStarted() {
            return !startPos.equals(NOT_STARTED);
        }

        byte getDirection() {
            return currentPos.getDirection(startPos);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);
        final LinearLayout background = findViewById(R.id.touchPadBackground);

        background.setBackgroundColor(Color.argb(255,
                0, 0, 0));
/*
        new Thread() {
            int color;
            public void run() {
                for (color = 255; color >= 0; color -= 3) {
                    try {
                        sleep(20);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                background.setBackgroundColor(Color.argb(255,
                                        color, color, color));
                            }
                        });
                    } catch (InterruptedException e) {
                        background.setBackgroundColor(Color.argb(255,
                                0, 0, 0));
                    }
                }
            }
        }.start();
 */

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        exitDialog = new AlertDialog.Builder(ActivityTouchPad.this)
                .setTitle("Warning")
                .setMessage("Do you want to exit touching pad?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        PermanentConnection.sendMessage("X");
                        finish();
                    }
                });

        if (Controls.getSetting(null, 'O').equals("VERTICAL")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        forwardScrollMode = Controls.getSetting(null, 'S').equals("FORWARD");
        touchWarningMode = Controls.getSetting(null, 'T').equals("ENABLED");

        eventGroup[0] = new FingerEvent(1.2F);
        eventGroup[1] = new FingerEvent(20F);
        eventGroup[2] = new FingerEvent(60F);
        eventGroup[3] = new FingerEvent(60F);
        eventGroup[4] = new FingerEvent(60F);
        // eventGroup[5] = new FingerEvent(1F);
        // eventGroup[6] = new FingerEvent(1F);
        // eventGroup[7] = new FingerEvent(1F);
        // eventGroup[8] = new FingerEvent(1F);
        // eventGroup[9] = new FingerEvent(1F);

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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            float currentX = event.getRawX();
            float currentY = event.getRawY();
            if (currentNumPointers != event.getPointerCount()) {
                for (FingerEvent fingerEvent : eventGroup) {
                    fingerEvent.setCurrentPos(currentX, currentY);
                }
            }
            currentNumPointers = event.getPointerCount();
            maxNumPointers = Math.max(currentNumPointers, maxNumPointers);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                maxNumPointers = 1;
                direction = -1;
                for (FingerEvent fingerEvent : eventGroup) {
                    fingerEvent.reset();
                }
                PermanentConnection.identifyAndSend(Controls.MOVE_CANCEL);
            }
            if (mDetector.onTouchEvent(event)) {
                switch (maxNumPointers) {
                    case 1:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            eventGroup[0].startEvent(currentX, currentY);
                            // Toast.makeText(ActivityTouchPad.this, "Down" + maxNumPointers, Toast.LENGTH_SHORT).show();
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            CoordinatePair pair = eventGroup[0].moveTo(currentX, currentY);
                            int deltaX = (int) pair.getX();
                            int deltaY = (int) pair.getY();
                            if (!pair.equals(ZERO)) {
                                PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), deltaX, deltaY);
                            }
                        }
                        break;
                    case 2:
                        // Two Fingers MOVE event
                        CoordinatePair motion2 = eventGroup[1].moveTo(currentX, currentY);
                        if (!((int) motion2.getY() == 0)) {
                            if (forwardScrollMode) {
                                PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.MOVE), (int) motion2.getY());
                            } else {
                                PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.MOVE), -(int) motion2.getY());
                            }
                        }
                        break;
                    case 3:
                        CoordinatePair motion3 = eventGroup[2].moveTo(currentX, currentY);
                        if (!motion3.equals(ZERO)) {
                            int setDirection = motion3.getDirection(ZERO);
                            if (direction == -1
                                    || ((setDirection == Controls.MOVE_LEFT || setDirection == Controls.MOVE_RIGHT)
                                    && (direction == Controls.MOVE_LEFT || direction == Controls.MOVE_RIGHT))
                                    || ((setDirection == Controls.MOVE_UP || setDirection == Controls.MOVE_DOWN)
                                    && (direction == Controls.MOVE_UP || direction == Controls.MOVE_DOWN))) {
                                PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + direction), setDirection);
                                direction = setDirection;
                            }
                        }
                        break;
                    case 4:
                        CoordinatePair motion4 = eventGroup[3].moveTo(currentX, currentY);
                        if (!motion4.equals(ZERO)) {
                            // Toast.makeText(ActivityTouchPad.this, direction, Toast.LENGTH_SHORT).show();
                            int setDirection = motion4.getDirection(ZERO);
                            if (direction == -1) {
                                PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + direction), setDirection);
                                direction = setDirection;
                            }
                            if (((setDirection == Controls.MOVE_LEFT || setDirection == Controls.MOVE_RIGHT)
                                    && (direction == Controls.MOVE_LEFT || direction == Controls.MOVE_RIGHT))) {
                                PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + direction), setDirection);
                                direction = setDirection;
                            }
                            if (setDirection == Controls.MOVE_UP && direction != 0) {
                                direction = 0;
                                exitDialog.show();
                            } else if (setDirection == Controls.MOVE_DOWN && direction != 0) {
                                direction = 0;
                                new AlertDialog.Builder(ActivityTouchPad.this)
                                        .setTitle("Warning")
                                        .setMessage("Do you want to enter settings?\nThis will cause current activity to be finished.")
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                PermanentConnection.sendMessage("X");
                                                Intent intent = new Intent(ActivityTouchPad.this, ActivitySettings.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }).show();
                            }
                        }
                        break;
                    case 5:
                        CoordinatePair motion5 = eventGroup[4].moveTo(currentX, currentY);
                        if (!motion5.equals(ZERO)) {
                            int setDirection = motion5.getDirection(ZERO);
                            if (direction == -1
                                    || ((setDirection == Controls.MOVE_LEFT || setDirection == Controls.MOVE_RIGHT)
                                    && (direction == Controls.MOVE_LEFT || direction == Controls.MOVE_RIGHT))
                                    || ((setDirection == Controls.MOVE_UP || setDirection == Controls.MOVE_DOWN)
                                    && (direction == Controls.MOVE_UP || direction == Controls.MOVE_DOWN))) {
                                PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + direction), setDirection);
                                direction = setDirection;
                            }
                        }
                        break;
                    default:
                        if (direction == -1) {
                            Toast.makeText(ActivityTouchPad.this, maxNumPointers + "", Toast.LENGTH_LONG).show();
                            if (touchWarningMode) {
                                new AlertDialog.Builder(ActivityTouchPad.this)
                                        .setTitle("Warning")
                                        .setMessage("Touching actions with more than 5 fingers is disabled for accuracy reason.\n"
                                                + "To disable this message, please go to settings.\n"
                                                + "If you wish to go to settings, current activity will be finished.")
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                PermanentConnection.sendMessage("X");
                                                Intent intent = new Intent(ActivityTouchPad.this, ActivitySettings.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }).show();
                            }
                            direction = 0;
                        }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onDown(final MotionEvent event) {
        final Handler SingleTapHandler = new Handler();
        SingleTapHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (maxNumPointers) {
                    case 2:
                        eventGroup[1].startEvent(event.getRawX(), event.getRawY());
                        final Handler twoFingerSingleTapConfirmedHandler = new Handler();
                        twoFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 2) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        final Handler twoFingerLongPressHandler = new Handler();
                        twoFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 2 && eventGroup[1].getDistanceLessThan(100)) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.LONG_PRESS));
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 3:
                        eventGroup[2].startEvent(event.getRawX(), event.getRawY());
                        final Handler threeFingerSingleTapConfirmedHandler = new Handler();
                        threeFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 3) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        final Handler threeFingerLongPressHandler = new Handler();
                        threeFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 3 && eventGroup[2].getDistanceLessThan(100)) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + Controls.LONG_PRESS));
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 4:
                        eventGroup[3].startEvent(event.getRawX(), event.getRawY());
                        final Handler fourFingerSingleTapConfirmedHandler = new Handler();
                        fourFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 4) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        final Handler fourFingerLongPressHandler = new Handler();
                        fourFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 4 && eventGroup[3].getDistanceLessThan(100)) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.LONG_PRESS));
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 5:
                        eventGroup[4].startEvent(event.getRawX(), event.getRawY());
                        final Handler fiveFingerSingleTapConfirmedHandler = new Handler();
                        fiveFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 5) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FIVE_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        final Handler fiveFingerLongPressHandler = new Handler();
                        fiveFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 5 && eventGroup[4].getDistanceLessThan(100)) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FIVE_FINGERS + Controls.LONG_PRESS));
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                }
            }
        }, 6);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.LONG_PRESS));
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {

        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.DOUBLE_TAP));
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.TAP));
        return true;
    }

    @Override
    public void onBackPressed() {
        exitDialog.show();
    }
}