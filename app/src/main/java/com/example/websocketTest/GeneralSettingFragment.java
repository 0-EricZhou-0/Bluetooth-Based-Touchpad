package com.example.websocketTest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralSettingFragment extends Fragment {

    private View rootView;
    private LinearLayout generalSettingContainer;

    private void loadSetting() {
        generalSettingContainer.removeAllViews();
        final ArrayList<Controls.SettingDetail> currentSettings = Controls.getCurrentSettings();
        for (final Controls.SettingDetail setting : currentSettings) {
            final View generalSetting = getLayoutInflater().inflate(R.layout.chunk_general_setting,
                    generalSettingContainer, false);
            final TextView stateDescription = generalSetting.findViewById(R.id.stateDescription);
            Button changeState = generalSetting.findViewById(R.id.changeSetting);
            TextView detailedDescription = generalSetting.findViewById(R.id.detailedDescription);
            stateDescription.setText(setting.getSettingDescriptionAndState());
            changeState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setItems(setting.getAllStatus(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setting.changeSetting(which);
                                    stateDescription.setText(setting.getSettingDescriptionAndState());
                                }
                            }).show();
                }
            });
            detailedDescription.setText(setting.getDetailedDescription());
            generalSettingContainer.addView(generalSetting);
        }
    }

    public GeneralSettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        generalSettingContainer = rootView.findViewById(R.id.generalSettings);
        loadSetting();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_general_setting, container, false);
        return rootView;
    }

}
