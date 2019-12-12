package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class ActivityAddNewMapping extends AppCompatActivity {

    private byte combinedAction;
    private String taskType;
    Intent returnIntent = new Intent();
    private SparseArray<Controls.TaskDetail> currentSettings;
    private AlertDialog.Builder exitDialog;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_mapping);
        Button abortAndBack = findViewById(R.id.abortAndBack);
        abortAndBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDialog.show();
            }
        });
        currentSettings = Controls.getCurrentMapping();

        exitDialog = new AlertDialog.Builder(ActivityAddNewMapping.this)
                .setTitle("Warning")
                .setMessage("Abort current mapping?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        setResult(Activity.RESULT_CANCELED, returnIntent);
                        finish();
                    }
                });

        final LinearLayout mappingSettings = findViewById(R.id.mappingSettings);
        final TextView currentChoiceDescription = findViewById(R.id.currentChoiceDescription);
        final TextView currentMappingInfo = findViewById(R.id.currentMappingInfo);
        currentChoiceDescription.setText("\n Select number of finger(s) used in the action\n");
        try {
            for (int i = 2; i < 5; i++) {
                final int fingerCount = i + 1;
                final View numFingerSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                        mappingSettings, false);
                final TextView numOfFingers = numFingerSelection.findViewById(R.id.descripiton);
                final Button selectNumFingers = numFingerSelection.findViewById(R.id.performAction);
                numOfFingers.setText(" " + fingerCount + " Finger");
                selectNumFingers.setText("SELECT");
                selectNumFingers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentChoiceDescription.setText("\n Select finger action type\n"
                                + "(Same number of fingers cannot have event Move and event Move Left/Right/Up/Down at the same time)");
                        currentMappingInfo.setText(" Currently Selected\n  " + numOfFingers.getText());
                        combinedAction = (byte) (fingerCount * 8);
                        mappingSettings.removeAllViews();
                        for (int i = Controls.TAP; i <= Controls.MOVE_DOWN; i++) {
                            if (i == Controls.MOVE || ((i == Controls.MOVE_LEFT || i == Controls.MOVE_RIGHT
                                    || i == Controls.MOVE_UP || i == Controls.MOVE_DOWN)
                                    && currentSettings.indexOfKey(combinedAction + Controls.MOVE) > -1)) {
                                continue;
                            }

                            final int actionNum = i;
                            final View actionTypeSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                                    mappingSettings, false);
                            final TextView actionType = actionTypeSelection.findViewById(R.id.descripiton);
                            final Button selectActionType = actionTypeSelection.findViewById(R.id.performAction);
                            actionType.setText(" " + Controls.getReadableDefinedAction((byte) i, null));
                            if (currentSettings.get(combinedAction + i) != null) {
                                selectActionType.setText("REPLACE");
                            } else {
                                selectActionType.setText("SELECT");
                            }
                            selectActionType.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    currentChoiceDescription.setText("\n Select mapping action type\n");
                                    currentMappingInfo.setText("" + currentMappingInfo.getText() + actionType.getText());
                                    combinedAction += actionNum;
                                    mappingSettings.removeAllViews();
                                    Set<String> allActions = Controls.getAllOuterControls();
                                    for (final String action : allActions) {
                                        final String readableAction = Controls.getReadableTask(action);
                                        if (readableAction.contains("Basic")
                                                || readableAction.contains("Switch") && (actionNum != Controls.MOVE_LEFT && actionNum != Controls.MOVE_RIGHT)) {
                                            continue;
                                        }
                                        final View mappingActionTypeSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                                                mappingSettings, false);
                                        final TextView mappingActionType = mappingActionTypeSelection.findViewById(R.id.descripiton);
                                        final Button selectMappingActionType = mappingActionTypeSelection.findViewById(R.id.performAction);
                                        mappingActionType.setText(" " + readableAction);
                                        selectMappingActionType.setText("SELECT");
                                        selectMappingActionType.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                taskType = action;
                                                final String addition;
                                                if (actionNum == Controls.MOVE_LEFT && readableAction.contains("Switch")) {
                                                    addition = "This will also add " + fingerCount + " Finger Move Right to " + readableAction;
                                                } else if (actionNum == Controls.MOVE_RIGHT && readableAction.contains("Switch")) {
                                                    addition = "This will also add " + fingerCount + " Finger Move Left to " + readableAction;
                                                } else {
                                                    addition = "";
                                                }
                                                new AlertDialog.Builder(ActivityAddNewMapping.this)
                                                        .setTitle("Confirm")
                                                        .setMessage("Add following mapping?\n" + currentMappingInfo.getText() + " --- " + readableAction + "\n" + addition)
                                                        .setNegativeButton("Abort", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                setResult(Activity.RESULT_CANCELED, returnIntent);
                                                                finish();
                                                            }
                                                        })
                                                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                            }
                                                        })
                                                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface arg0, int arg1) {
                                                                returnIntent.putExtra("combinedAction", combinedAction);
                                                                returnIntent.putExtra("taskType", taskType);
                                                                if (!addition.equals("")) {
                                                                    if (actionNum == Controls.MOVE_LEFT) {
                                                                        returnIntent.putExtra("bundleAction", (byte) (combinedAction + 1));
                                                                    } else {
                                                                        returnIntent.putExtra("bundleAction", (byte) (combinedAction - 1));
                                                                    }
                                                                }
                                                                setResult(Activity.RESULT_OK, returnIntent);
                                                                finish();
                                                            }
                                                        }).show();
                                            }
                                        });
                                        mappingSettings.addView(mappingActionTypeSelection);
                                    }
                                }
                            });
                            mappingSettings.addView(actionTypeSelection);
                        }
                    }
                });
                mappingSettings.addView(numFingerSelection);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        exitDialog.show();
    }
}
