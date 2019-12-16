package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;

public class ActivitySettings extends AppCompatActivity {

    private static final int ADD_MAPPING = 0;

    private GestureDetectorCompat mDetector;
    ArrayList<View> actionList = new ArrayList<>();
    SparseArray<Controls.TaskDetail> currentSettings = new SparseArray<>();
    SparseBooleanArray currentGeneralSetting = Controls.getCurrentSettingStatus();
    private LinearLayout container;
    private AlertDialog.Builder exitDialog;
    private boolean modification = false;

    class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            float xChange = event2.getRawX() - event1.getRawX();
            float yChange = event2.getRawY() - event1.getRawY();
            if (xChange - Math.abs(yChange) > 50) {
                if (modification) {
                    exitDialog.show();
                } else {
                    finish();
                }
            }
            return true;
        }
    }

    @SuppressLint("SetTextI18n")
    private void setSettingButtonText(Button settingsButton, char setting) {
        settingsButton.setText(Controls.getButtonDescription(setting) + ": " + Controls.getSetting(currentGeneralSetting, setting));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void reload() {
        try {
            container.removeAllViews();
            for (int i = 0; i < currentSettings.size(); i++) {
                Controls.TaskDetail mapping = currentSettings.valueAt(i);
                final byte combinedAction = (byte) currentSettings.keyAt(i);
                String description = Controls.getReadableDefinedAction(combinedAction, mapping);
                if (description == null) {
                    continue;
                }
                final View keyMapping = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                        container, false);
                actionList.add(keyMapping);
                TextView mappingDescription = keyMapping.findViewById(R.id.descripiton);
                Button removeMapping = keyMapping.findViewById(R.id.performAction);
                if (description.contains("Basic")) {
                    removeMapping.setVisibility(View.INVISIBLE);
                } else {
                    removeMapping.setText("REMOVE");
                    removeMapping.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(ActivitySettings.this)
                                    .setTitle("Warning")
                                    .setMessage("Remove this mapping?")
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            modification = true;
                                            actionList.remove(keyMapping);
                                            container.removeView(keyMapping);
                                            currentSettings.remove(combinedAction);
                                        }
                                    }).show();
                        }
                    });
                }
                mappingDescription.setText(description);
                container.addView(keyMapping);
            }
        } catch (Exception e) {
            Toast.makeText(ActivitySettings.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mDetector = new GestureDetectorCompat(this, new CustomGestureListener());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Button orientationSetting = findViewById(R.id.orientationSetting);
        final Button scrollModeSetting = findViewById(R.id.scrollModeSetting);
        final Button touchWarningSetting = findViewById(R.id.touchWarningSetting);
        final Button cursorModeSetting = findViewById(R.id.cursorModeSetting);
        final Button addNewMapping = findViewById(R.id.addNewMapping);
        final LinearLayout definedAction = findViewById(R.id.definedAction);

        exitDialog = new AlertDialog.Builder(ActivitySettings.this)
                .setTitle("Warning")
                .setMessage("Save Changes?")
                .setNegativeButton("Abort", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        Controls.setCurrentSettingStatus(currentGeneralSetting);
                        Controls.remapping(currentSettings);
                        finish();
                    }
                });

        addNewMapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivitySettings.this, ActivityAddNewMapping.class);
                startActivityForResult(intent, ADD_MAPPING);
                modification = true;
            }
        });

        currentSettings = Controls.getCurrentMapping();
        container = definedAction;
        reload();

        setSettingButtonText(orientationSetting, 'O');
        setSettingButtonText(scrollModeSetting, 'S');
        setSettingButtonText(touchWarningSetting, 'T');
        setSettingButtonText(cursorModeSetting, 'C');

        orientationSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentGeneralSetting.put('O', !currentGeneralSetting.get('O'));
                setSettingButtonText(orientationSetting, 'O');
            }
        });

        scrollModeSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentGeneralSetting.put('S', !currentGeneralSetting.get('S'));
                setSettingButtonText(scrollModeSetting, 'S');
            }
        });

        touchWarningSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentGeneralSetting.put('T', !currentGeneralSetting.get('T'));
                setSettingButtonText(touchWarningSetting, 'T');
            }
        });

        cursorModeSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean cursorMode = currentGeneralSetting.get('C');
                currentGeneralSetting.put('C', !cursorMode);
                if (cursorMode) {
                    Controls.changeCursorMoveMode(false);
                } else {
                    Controls.changeCursorMoveMode(true);
                }
                setSettingButtonText(cursorModeSetting, 'C');
                reload();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ADD_MAPPING) {
            if (resultCode == ActivitySettings.RESULT_OK) {
                byte combinedAction = data.getByteExtra("combinedAction", (byte) -1);
                byte bundleAction = data.getByteExtra("bundleAction", (byte) -1);
                String taskType = data.getStringExtra("taskType");
                Controls.TaskDetail detail = Controls.TaskDetail.correspondsTo(taskType);
                currentSettings.put(combinedAction, detail.duplicate());
                if (bundleAction != -1) {
                    currentSettings.put(bundleAction, detail.duplicate());
                }
                Controls.remapping(currentSettings);
                reload();
            } else {
                Toast.makeText(ActivitySettings.this, "Action Canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (modification) {
            exitDialog.show();
        } else {
            finish();
        }
    }
}
