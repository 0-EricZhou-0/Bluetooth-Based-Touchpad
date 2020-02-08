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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;

import java.util.List;

import static com.example.websocketTest.Controls.NOT_STARTED;
import static com.example.websocketTest.Controls.ZERO;

public class ActivityTouchPad extends AppCompatActivity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static final String TAG = "TouchPad";
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
    private final int WAIT_UNTIL_CONFIRM = 450;

    /**
     * The number of milliseconds waited until it is sure that the user is trying to long press.
     */
    private final int LONG_PRESS_LENGTH = 600;

    @SuppressWarnings("FieldCanBeLocal")
    private final int MAXIMUM_ALLOWANCE_BEFORE_RECOGNIZING = 15;

    private GestureDetectorCompat mDetector;
    private InputMethodManager inputMethod;

    private EditText keyboardInput;

    private FingerEvent[] eventGroup = new FingerEvent[MAX_NUM_OF_FINGERS_SUPPORTED];

    private int maxNumPointers = 1;
    private int currentNumPointers = 1;
    private int direction = -1;

    private boolean isSinglePress;
    private long lastAccessed;
    private boolean lastMoved;
    private boolean canDetectDoublePress = true;

    private boolean forwardScrollMode;
    private boolean touchWarningMode;
    private boolean cursorMode;

    private AlertDialog exitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);
        final ConstraintLayout background = findViewById(R.id.touchPadBackground);
        Controls.WindowManagement.enableImmersiveMode(this);
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

        keyboardInput = findViewById(R.id.keyboardInput);
        Button pasteFromClipboard = findViewById(R.id.pasteFromClipboard);

        inputMethod = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        pasteFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String content = Controls.getClipboardContent();
                if (content == null) {
                    Toast.makeText(ActivityTouchPad.this, R.string.noContentInClipboard, Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(ActivityTouchPad.this)
                            .setTitle(R.string.pasteFromClipboard)
                            .setMessage(Controls.getClipboardContent())
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PermanentConnection.identifyAndSend(Controls.INPUT_TEXT, content);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            }).show();
                }
            }
        });
        exitDialog = new AlertDialog.Builder(ActivityTouchPad.this)
                .setTitle(R.string.warning)
                .setMessage(R.string.exitTouchPad)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.MOVE_DOWN));
                        finish();
                    }
                }).create();


        if (Controls.SettingDetail.getStatusOfSetting(Controls.ORIENTATION_SETTING).equals(getString(R.string.vertical))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        forwardScrollMode = Controls.SettingDetail.getStatusOfSetting(Controls.SCROLL_MODE_SETTING).equals(getString(R.string.forward));
        touchWarningMode = Controls.SettingDetail.getStatusOfSetting(Controls.TOUCH_WARNING_SETTING).equals(getString(R.string.enabled));
        cursorMode = Controls.SettingDetail.getStatusOfSetting(Controls.CURSOR_MODE_SETTING).equals(getString(R.string.relative));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Controls.phoneScreenSize = new CoordinatePair(background.getWidth(), background.getHeight());
            }
        }, 100);

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
                }, 2500);
            }
        });

        keyboardInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i(TAG, String.format("Text changed, current s: |%s|", s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "Type character " + s.toString());
                PermanentConnection.identifyAndSend(Controls.INPUT_TEXT, s.toString().substring(1));
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
                            isSinglePress = System.currentTimeMillis() - lastAccessed >= WAIT_UNTIL_CONFIRM;
                            Log.i(TAG, "Single Press? " + isSinglePress);
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            lastMoved = true;
                            CoordinatePair pair = eventGroup[0].moveTo(currentX, currentY);
                            if (!pair.equals(ZERO)) {
                                if (cursorMode) {
                                    int deltaX = (int) pair.getX();
                                    int deltaY = (int) pair.getY();
                                    PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), deltaX, deltaY);
                                } else {
                                    int targetX = (int) (currentX / Controls.phoneScreenSize.getX() * 10000);
                                    int targetY = (int) (currentY / Controls.phoneScreenSize.getY() * 10000);
                                    PermanentConnection.identifyAndSend((byte) (Controls.SINGLE_FINGER + Controls.MOVE), targetX, targetY);
                                }
                            }
                        }
                        break;
                    case 2:
                        // Move up/down actions of two fingers
                        // Cannot be customized
                        CoordinatePair motion2 = eventGroup[1].moveTo(currentX, currentY);
                        if (!motion2.equals(ZERO)) {
                            lastMoved = true;
                            if (forwardScrollMode) {
                                PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.MOVE), (int) motion2.getX(), (int) motion2.getY());
                            } else {
                                PermanentConnection.identifyAndSend((byte) (Controls.TWO_FINGERS + Controls.MOVE), (int) motion2.getX(), -(int) motion2.getY());
                            }
                        }
                        break;
                    case 3:
                        // Move left/right/up/down actions of three fingers
                        CoordinatePair motion3 = eventGroup[2].moveTo(currentX, currentY);
                        if (!motion3.equals(ZERO)) {
                            lastMoved = true;
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
                            lastMoved = true;
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
                                Controls.WindowManagement.showDialogSilence(exitDialog, this);
                            }
                            if (setDirection == Controls.MOVE_UP && direction != 0) {
                                Log.i(TAG, "On-Screen keyboard show");
                                direction = 0;
                                keyboardInput.requestFocusFromTouch();
                                inputMethod.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                            }
                        }
                        break;
                    case 5:
                        // Move left/right/up/down actions of five fingers
                        CoordinatePair motion5 = eventGroup[4].moveTo(currentX, currentY);
                        if (!motion5.equals(ZERO)) {
                            lastMoved = true;
                            int setDirection = motion5.getDirection(ZERO);
                            if (direction == -1
                                    || ((setDirection == Controls.MOVE_LEFT || setDirection == Controls.MOVE_RIGHT)
                                    && (direction == Controls.MOVE_LEFT || direction == Controls.MOVE_RIGHT))
                                    || ((setDirection == Controls.MOVE_UP || setDirection == Controls.MOVE_DOWN)
                                    && (direction == Controls.MOVE_UP || direction == Controls.MOVE_DOWN))) {
                                PermanentConnection.identifyAndSend((byte) (Controls.FIVE_FINGERS + direction), setDirection);
                                direction = setDirection;
                            }
                        }
                        break;
                    default:
                        // Touch actions more than 5 fingers are not recommended.
                        if (direction == -1) {
                            Toast.makeText(ActivityTouchPad.this, maxNumPointers + "", Toast.LENGTH_LONG).show();
                            if (touchWarningMode) {
                                Controls.WindowManagement.showDialogSilence(
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
                                                PermanentConnection.identifyAndSend((byte) (Controls.FOUR_FINGERS + Controls.MOVE_DOWN));
                                                Intent intent = new Intent(ActivityTouchPad.this, ActivitySettings.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }).create(), this);
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
        final boolean moved = lastMoved;
        canDetectDoublePress = !lastMoved;
        Log.i(TAG, "Last moved: " + lastMoved + " at " + System.currentTimeMillis());
        lastMoved = false;
        final Handler tapHandler = new Handler();
        tapHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int currentMaxNumPointers = maxNumPointers;
                if (currentMaxNumPointers == 1) {
                    return;
                }
                eventGroup[currentMaxNumPointers - 1].startEvent(event.getRawX(), event.getRawY());
                final byte fingerNumInnerControl = Controls.convertNumFingersToInnerControl(maxNumPointers);
                final Handler pressHandler = new Handler();
                final Runnable confirmLongPress = new Runnable() {
                    @Override
                    public void run() {
                        if (moved) {
                            Log.i(TAG, currentMaxNumPointers + " Finger long press examination aborted due to moved before. In " + pressHandler.toString());
                        } else {
                            Log.i(TAG, currentMaxNumPointers + " Finger long press examination started in " + pressHandler.toString());
                            if (maxNumPointers == currentMaxNumPointers && eventGroup[currentMaxNumPointers - 1].isPointerNotMoved()) {
                                Log.i(TAG, currentMaxNumPointers + " Finger long press confirmed in " + pressHandler.toString());
                                PermanentConnection.identifyAndSend((byte) (fingerNumInnerControl + Controls.LONG_TAP));
                                Controls.vibrate();
                            }
                        }
                    }
                };
                final Runnable confirmSingleAndDoubleTap = new Runnable() {
                    @Override
                    public void run() {
                        if (moved) {
                            Log.i(TAG, currentMaxNumPointers + " Finger single/double press examination aborted due to moved before. In " + pressHandler.toString());
                            return;
                        }
                        Log.i(TAG, currentMaxNumPointers + " Finger single/double press examination started in " + pressHandler.toString());
                        if (!isSinglePress && canDetectDoublePress) {
                            Log.i(TAG, currentMaxNumPointers + " Finger double press confirmed in " + pressHandler.toString());
                            isSinglePress = true;
                            Log.i(TAG, "Double pressed assign at " + System.currentTimeMillis());
                            pressHandler.removeCallbacks(confirmLongPress);
                            PermanentConnection.identifyAndSend((byte) (fingerNumInnerControl + Controls.DOUBLE_TAP));
                        } else if (maxNumPointers < currentMaxNumPointers) {
                            Log.i(TAG, currentMaxNumPointers + " Finger single press confirmed in " + pressHandler.toString());
                            PermanentConnection.identifyAndSend((byte) (fingerNumInnerControl + Controls.TAP));
                            pressHandler.removeCallbacks(confirmLongPress);
                        }
                    }
                };

                // Single/double tap confirmed
                pressHandler.postDelayed(confirmSingleAndDoubleTap, WAIT_UNTIL_CONFIRM);
                // Long press confirmed
                pressHandler.postDelayed(confirmLongPress, LONG_PRESS_LENGTH);
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
        if (eventGroup[0].isPointerNotMoved()) {
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

        boolean isPointerNotMoved() {
            return startPos.getDistance(currentPos) < MAXIMUM_ALLOWANCE_BEFORE_RECOGNIZING;
        }
/*
        boolean hasNotStarted() {
            return !startPos.equals(NOT_STARTED);
        }

        byte getDirection() {
            return currentPos.getDirection(startPos);
        }
 */
    }
}
