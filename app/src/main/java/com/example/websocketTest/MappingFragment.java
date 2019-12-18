package com.example.websocketTest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class MappingFragment extends Fragment {

    private View rootView;
    private LinearLayout mappingContainer;
    private SparseArray<Controls.TaskDetail> currentMappings;
    private ArrayList<View> actionList = new ArrayList<>();

    private void reloadMapping() {
        mappingContainer.removeAllViews();
        for (int i = 0; i < currentMappings.size(); i++) {
            Controls.TaskDetail mapping = currentMappings.valueAt(i);
            final byte combinedAction = (byte) currentMappings.keyAt(i);
            final String description = Controls.getReadableDefinedAction(combinedAction, mapping);
            if (description == null) {
                continue;
            }
            final View keyMapping = getLayoutInflater().inflate(R.layout.chunk_mapping,
                    mappingContainer, false);
            actionList.add(keyMapping);
            TextView mappingDescription = keyMapping.findViewById(R.id.descripiton);
            Button removeMapping = keyMapping.findViewById(R.id.performAction);
            if (description.contains(getString(R.string.basicControl))) {
                removeMapping.setVisibility(View.INVISIBLE);
            } else {
                removeMapping.setText(R.string.remove);
                removeMapping.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.warning)
                                .setMessage(R.string.removeMapping)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        if (description.contains(getString(R.string.switchControl))) {
                                            int idx = actionList.indexOf(keyMapping);
                                            if (description.contains(getString(R.string.moveLeft))) {
                                                mappingContainer.removeView(actionList.remove(idx));
                                                mappingContainer.removeView(actionList.remove(idx));
                                                currentMappings.remove(combinedAction);
                                                currentMappings.remove(combinedAction + 1);
                                            } else {
                                                mappingContainer.removeView(actionList.remove(idx - 1));
                                                mappingContainer.removeView(actionList.remove(idx - 1));
                                                currentMappings.remove(combinedAction - 1);
                                                currentMappings.remove(combinedAction);
                                            }
                                        } else {
                                            actionList.remove(keyMapping);
                                            mappingContainer.removeView(keyMapping);
                                            currentMappings.remove(combinedAction);
                                        }
                                    }
                                }).show();
                    }
                });
            }
            mappingDescription.setText(description);
            mappingContainer.addView(keyMapping);
        }
    }

    public MappingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mappingContainer = rootView.findViewById(R.id.definedActionList);
        Button addNewMapping = rootView.findViewById(R.id.addNewMapping);
        currentMappings = Controls.getCurrentMappings();
        addNewMapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ActivityAddNewMapping.class);
                startActivityForResult(intent, Controls.ADD_MAPPING);
            }
        });
        reloadMapping();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_mapping, container, false);
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Controls.ADD_MAPPING) {
            if (resultCode == ActivitySettings.RESULT_OK) {
                byte combinedAction = data.getByteExtra("combinedAction", (byte) -1);
                byte bundleAction = data.getByteExtra("bundleAction", (byte) -1);
                String taskType = data.getStringExtra("taskType");
                Controls.TaskDetail detail = Controls.TaskDetail.correspondsTo(taskType);
                currentMappings.put(combinedAction, detail.duplicate());
                if (bundleAction != -1) {
                    currentMappings.put(bundleAction, detail.duplicate());
                }
                reloadMapping();
            } else {
                Toast.makeText(getActivity(), "Action Canceled", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
