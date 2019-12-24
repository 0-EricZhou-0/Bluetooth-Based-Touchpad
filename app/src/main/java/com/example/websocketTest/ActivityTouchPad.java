package com.example.websocketTest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import java.util.List;

import static com.example.websocketTest.Controls.NOT_STARTED;
import static com.example.websocketTest.Controls.ZERO;

public class ActivityTouchPad extends AppCompatActivity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    /**
     * The highest number of fingers that would be detected. Actions with higher number of fingers
     * will be discard.
     */
    private final int MAX_NUM_OF_FINGERS_SUPPORTED = 5;

    /**
     * When the user first touch the screen with fingers, the number of fingers touched initially
     * (usually one) would not be the number intended to do the action. The allowance measures the
     * number of milliseconds delayed before the handler actually detects the actual number of fingers
     * used to do the gesture. The value should not be less than 6, otherwise user will have a hard
     * time using the touch pad. And the value should not be too big, otherwise it may not successfully
     * detect the user actions.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final int DELAY_BEFORE_START_DETECTION = 15;

    /**
     * The number of milliseconds waited until it is sure that the user does not want to tap twice.
     */
    private final int WAIT_UNTIL_CONFIRM = 550;

    /**
     * The number of milliseconds waited until it is sure that the user is trying to long press.
     */
    private final int LONG_PRESS_LENGTH = 800;

    @SuppressWarnings("FieldCanBeLocal")
    private final int MAXIMUM_ALLOWANCE_BEFORE_RECOGNIZING = 15;

    private GestureDetectorCompat mDetector;
    private EditText keyboardInput;

    private FingerEvent[] eventGroup = new FingerEvent[MAX_NUM_OF_FINGERS_SUPPORTED];

    private int maxNumPointers = 1;
    private int currentNumPointers = 1;
    private int direction = -1;
    private boolean isSinglePress;
    private long lastAccessed;

    private boolean forwardScrollMode;
    private boolean touchWarningMode;
    private boolean cursorMode;

    private AlertDialog.Builder exitDialog;

    private class FingerEvent {
        private CoordinatePair startPos = NOT_STARTED;
        private CoordinatePair currentPos = NOT_STARTED;
        private float maximumTolerance;

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

        boolean getDistanceLessThan() {
            return startPos.getDistance(currentPos) < MAXIMUM_ALLOWANCE_BEFORE_RECOGNIZING;
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
        keyboardInput = new EditText(ActivityTouchPad.this);
        keyboardInput.setVisibility(View.INVISIBLE);
        background.addView(keyboardInput);

        exitDialog = new AlertDialog.Builder(ActivityTouchPad.this)
                .setTitle(R.string.warning)
                .setMessage(R.string.exitConfirm)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        PermanentConnection.sendMessage("X");
                        finish();
                    }
                });

        if (Controls.SettingDetail.getStatusOfSetting(Controls.ORIENTATION_SETTING).equals(getString(R.string.vertical))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        forwardScrollMode = Controls.SettingDetail.getStatusOfSetting(Controls.SCROLL_MODE_SETTING).equals(getString(R.string.forward));
        touchWarningMode = Controls.SettingDetail.getStatusOfSetting(Controls.TOUCH_WARNING_SETTING).equals(getString(R.string.enabled));
        cursorMode = Controls.SettingDetail.getStatusOfSetting(Controls.CURSOR_MODE_SETTING).equals(getString(R.string.relative));

        List<Controls.SensitivitySetting> currentSensitivities = Controls.getCurrentSensitivities();

        for (int i = 0; i < currentSensitivities.size(); i++) {
            eventGroup[i] = new FingerEvent(currentSensitivities.get(i).getRealSensitivity());
        }

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

        keyboardInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                PermanentConnection.sendMessage("K " + s.toString());
                s.clear();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            // The absolute position of the current cursor
            float currentX = event.getRawX();
            float currentY = event.getRawY();
            if (currentNumPointers != event.getPointerCount()) {
                // Prevent sudden jump of controls
                for (FingerEvent fingerEvent : eventGroup) {
                    fingerEvent.setCurrentPos(currentX, currentY);
                }
            }
            currentNumPointers = event.getPointerCount();
            maxNumPointers = Math.max(currentNumPointers, maxNumPointers);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Reset all action recorders
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
                        // Move event of single finger
                        // Cannot be customized
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            eventGroup[0].startEvent(currentX, currentY);
                            isSinglePress = System.currentTimeMillis() - lastAccessed >= LONG_PRESS_LENGTH;
                            Log.println(Log.INFO, "Touch Pad", "Single Press? " + isSinglePress);
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            CoordinatePair pair = eventGroup[0].moveTo(currentX, currentY);
                            if (!pair.equals(ZERO)) {
                                if (cursorMode) {
                                    int deltaX = (int) pair.getX();
                                    int deltaY = (int) pair.getY();
                                    PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), deltaX, deltaY);
                                } else {
                                    int targetX = (int) ((currentX * 0.9 / Controls.phoneScreenSize.getX() + 0.05) * 10000);
                                    int targetY = (int) ((currentY * 0.9 / Controls.phoneScreenSize.getY() + 0.05) * 10000);
                                    PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), targetX, targetY);
                                }
                            }
                        }
                        break;
                    case 2:
                        // Move up/down actions of two fingers
                        // Cannot be customized
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
                        // Move left/right/up/down actions of three fingers
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
                        // Move left/right/up/down actions of four fingers
                        // Four finger move up & down are reserved
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
                            if (setDirection == Controls.MOVE_DOWN && direction != 0) {
                                direction = 0;
                                exitDialog.show();
                            }
                            if (setDirection == Controls.MOVE_UP && direction != 0) {
                                Log.println(Log.INFO, "TouchPad", "On-Screen keyboard show");
                                direction = 0;
                                keyboardInput.requestFocus();
                                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                                        .showSoftInput(keyboardInput, InputMethodManager.SHOW_FORCED);
                            }
                        }
                        break;
                    case 5:
                        // Move left/right/up/down actions of five fingers
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
                        // Touch actions more than 5 fingers are not recommended.
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
                lastAccessed = System.currentTimeMillis();
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
                        // Two finger single tap confirmed
                        eventGroup[1].startEvent(event.getRawX(), event.getRawY());
                        final Handler twoFingerSingleTapConfirmedHandler = new Handler();
                        twoFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 2 && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        // Two finger long press
                        final Handler twoFingerLongPressHandler = new Handler();
                        twoFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 2 && eventGroup[1].getDistanceLessThan()) {
                                    if (isSinglePress) {
                                        PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.LONG_TAP));
                                        Controls.vibrate();
                                    } else {
                                        PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.DOUBLE_TAP));
                                    }
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 3:
                        // Three finger single tap confirmed
                        eventGroup[2].startEvent(event.getRawX(), event.getRawY());
                        final Handler threeFingerSingleTapConfirmedHandler = new Handler();
                        threeFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 3 && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        // Three finger long press
                        final Handler threeFingerLongPressHandler = new Handler();
                        threeFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 3 && eventGroup[2].getDistanceLessThan()) {
                                    if (isSinglePress) {
                                        PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + Controls.LONG_TAP));
                                        Controls.vibrate();
                                    } else {
                                        PermanentConnection.identifyAndSend((byte) (Controls.THREE_FINGERS + Controls.DOUBLE_TAP));
                                    }
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 4:
                        // Four finger single tap confirmed
                        eventGroup[3].startEvent(event.getRawX(), event.getRawY());
                        final Handler fourFingerSingleTapConfirmedHandler = new Handler();
                        fourFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 4 && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        // Four finger long press
                        final Handler fourFingerLongPressHandler = new Handler();
                        fourFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 4 && eventGroup[3].getDistanceLessThan() && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.LONG_TAP));
                                    Controls.vibrate();
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                    case 5:
                        // Five finger single tap confirmed
                        eventGroup[4].startEvent(event.getRawX(), event.getRawY());
                        final Handler fiveFingerSingleTapConfirmedHandler = new Handler();
                        fiveFingerSingleTapConfirmedHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers < 5 && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FIVE_FINGERS + Controls.TAP));
                                }
                            }
                        }, WAIT_UNTIL_CONFIRM);
                        // Five finger long press
                        final Handler fiveFingerLongPressHandler = new Handler();
                        fiveFingerLongPressHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (maxNumPointers == 5 && eventGroup[4].getDistanceLessThan() && isSinglePress) {
                                    PermanentConnection.identifyAndSend((byte) (Controls.FIVE_FINGERS + Controls.LONG_TAP));
                                    Controls.vibrate();
                                }
                            }
                        }, LONG_PRESS_LENGTH);
                        break;
                }
            }
        }, DELAY_BEFORE_START_DETECTION);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (isSinglePress) {
            PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.LONG_TAP));
            Controls.vibrate();
        }
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
        if (cursorMode) {
            PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.TAP));
        } else {
            int targetX = (int) (event.getRawX() / Controls.phoneScreenSize.getX() * 10000);
            int targetY = (int) (event.getRawY() / Controls.phoneScreenSize.getY() * 10000);
            PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), targetX, targetY);
            PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.TAP));
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        exitDialog.show();
    }
}
