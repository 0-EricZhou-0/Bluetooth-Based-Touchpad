package com.example.websocketTest;

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

import java.util.Locale;
import java.util.Set;

public class ActivityAddNewMapping extends AppCompatActivity {
    /**
     * The composition of action and finger number.
     * To see the action, see the Controls.java.
     */
    private byte actionFingerCount;

    /**
     * To see the task, see the Controls.java
     */
    private String taskType;

    /**
     * The intent used to return to ActivitySettings when the adding mapping process finished or aborted.
     */
    Intent returnIntent = new Intent();

    /**
     * Current mapping of combined action to task.
     */
    private SparseArray<Controls.TaskDetail> currentMapping;

    /**
     * The AlertDialog posted when trying to exit this page.
     */
    private AlertDialog.Builder exitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_mapping);

        currentMapping = Controls.getCurrentMapping();

        // Initiate exiting AlertDialog
        exitDialog = new AlertDialog.Builder(ActivityAddNewMapping.this)
                .setTitle(R.string.warning)
                .setMessage(R.string.abortCurrent)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        setResult(Activity.RESULT_CANCELED, returnIntent);
                        finish();
                    }
                });

        Button abortAndBack = findViewById(R.id.abortAndBack);
        abortAndBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDialog.show();
            }
        });

        final LinearLayout mappingSettings = findViewById(R.id.mappingSettings);
        final TextView currentChoiceDescription = findViewById(R.id.currentChoiceDescription);
        final TextView currentMappingInfo = findViewById(R.id.currentMappingInfo);
        currentChoiceDescription.setText(String.format("\n %s\n", getString(R.string.setFingerNumDescription)));
        try {
            for (int fingerCoding = 2; fingerCoding <= 5; fingerCoding++) {
                final int fingerCount = fingerCoding;
                final View numFingerSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                        mappingSettings, false);
                final TextView numOfFingers = numFingerSelection.findViewById(R.id.descripiton);
                final Button selectNumFingers = numFingerSelection.findViewById(R.id.performAction);
                numOfFingers.setText(String.format(Locale.getDefault(), " %d %s", fingerCount, getString(R.string.finger)));
                selectNumFingers.setText(R.string.select);
                selectNumFingers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentChoiceDescription.setText(String.format("\n %s\n", getString(R.string.setActionDescription)));
                        currentMappingInfo.setText(String.format(" %s\n  %s", getString(R.string.currentSelected), numOfFingers.getText()));
                        actionFingerCount = (byte) (fingerCount * 8);
                        mappingSettings.removeAllViews();
                        for (int actionCoding = Controls.TAP; actionCoding <= Controls.MOVE_DOWN; actionCoding++) {
                            final int action = actionCoding;

                            if (action == Controls.MOVE || ((action == Controls.MOVE_LEFT || action == Controls.MOVE_RIGHT
                                    || action == Controls.MOVE_UP || action == Controls.MOVE_DOWN)
                                    && currentMapping.indexOfKey(actionFingerCount + Controls.MOVE) > -1)) {
                                continue;
                            }

                            final View actionTypeSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                                    mappingSettings, false);
                            final TextView actionType = actionTypeSelection.findViewById(R.id.descripiton);
                            final Button selectActionType = actionTypeSelection.findViewById(R.id.performAction);
                            actionType.setText(String.format(" %s", Controls.getReadableDefinedAction((byte) action, null)));
                            if (currentMapping.get(actionFingerCount + action) != null) {
                                selectActionType.setText(R.string.replace);
                            } else {
                                selectActionType.setText(R.string.select);
                            }
                            selectActionType.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    currentChoiceDescription.setText(String.format("\n %s\n", getString(R.string.selectTask)));
                                    currentMappingInfo.setText(String.format("%s %s", currentMappingInfo.getText(), actionType.getText()));
                                    final byte combinedAction = (byte) (actionFingerCount + action);
                                    mappingSettings.removeAllViews();
                                    Set<String> allActions = Controls.getAllTasks();
                                    for (final String individualAction : allActions) {
                                        final String readableAction = Controls.getReadableTask(individualAction);
                                        if (readableAction.contains(getString(R.string.basicControl))
                                                || readableAction.contains(getString(R.string.switchControl)) && (action != Controls.MOVE_LEFT && action != Controls.MOVE_RIGHT)) {
                                            continue;
                                        }
                                        final View taskTypeSelection = getLayoutInflater().inflate(R.layout.chunk_mapping_setting,
                                                mappingSettings, false);
                                        final TextView taskType = taskTypeSelection.findViewById(R.id.descripiton);
                                        final Button selectTaskType = taskTypeSelection.findViewById(R.id.performAction);
                                        taskType.setText(String.format(" %s", readableAction));
                                        selectTaskType.setText(R.string.select);
                                        selectTaskType.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ActivityAddNewMapping.this.taskType = individualAction;
                                                final String addition;
                                                boolean containsSwitch = readableAction.contains(getString(R.string.switchControl));
                                                if (action == Controls.MOVE_LEFT && containsSwitch) {
                                                    addition = String.format(Locale.getDefault(),
                                                            "%s %d %s %s %s %s", getString(R.string.bindAdding), fingerCount, getString(R.string.finger),
                                                            getString(R.string.moveRight), getString(R.string.to), readableAction);
                                                } else if (action == Controls.MOVE_RIGHT && containsSwitch) {
                                                    addition = String.format(Locale.getDefault(),
                                                            "%s %d %s %s %s %s", getString(R.string.bindAdding), fingerCount, getString(R.string.finger),
                                                            getString(R.string.moveLeft), getString(R.string.to), readableAction);
                                                } else {
                                                    addition = "";
                                                }
                                                new AlertDialog.Builder(ActivityAddNewMapping.this)
                                                        .setTitle(R.string.confirm)
                                                        .setMessage(String.format("%s\n%s --- %s\n%s",getString(R.string.addMapping), currentMappingInfo.getText(), readableAction, addition))
                                                        .setNegativeButton(R.string.abort, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                setResult(Activity.RESULT_CANCELED, returnIntent);
                                                                finish();
                                                            }
                                                        })
                                                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                            }
                                                        })
                                                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface arg0, int arg1) {
                                                                returnIntent.putExtra("combinedAction", combinedAction);
                                                                returnIntent.putExtra("taskType", ActivityAddNewMapping.this.taskType);
                                                                if (!addition.equals("")) {
                                                                    if (action == Controls.MOVE_LEFT) {
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
                                        mappingSettings.addView(taskTypeSelection);
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
