package com.example.websocketTest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralSettingFragment extends Fragment {

    private View rootView;
    private LinearLayout generalSettingContainer;

    private void loadSetting() {
        generalSettingContainer.removeAllViews();
        final List<Controls.SettingDetail> currentSettings = Controls.getCurrentSettings();
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
                                    Controls.updateAllSetting(false);
                                }
                            }).show();
                }
            });
            detailedDescription.setText(setting.getDetailedDescription());
            generalSettingContainer.addView(generalSetting);
        }

        final TextView sensitivityHeading = new TextView(getContext());
        sensitivityHeading.setTypeface(Typeface.DEFAULT_BOLD);
        sensitivityHeading.setText(String.format("\n\n%s", getString(R.string.sensitivityHeading)));
        generalSettingContainer.addView(sensitivityHeading);

        final List<Controls.SensitivitySetting> currentSensitivities = Controls.getCurrentSensitivities();
        int numFingers = 0;
        for (final Controls.SensitivitySetting sensitivity : currentSensitivities) {
            final View sensitivitySetting = getLayoutInflater().inflate(R.layout.chunk_sensitivity_setting,
                    generalSettingContainer, false);
            final TextView sensitivityDescription = sensitivitySetting.findViewById(R.id.sensitivityDescription);
            final SeekBar sensitivitySeekBar = sensitivitySetting.findViewById(R.id.sensitivitySeekBar);
            final String description = String.format(Locale.getDefault(), " %d %s", ++numFingers, getString(R.string.finger));
            sensitivityDescription.setText(String.format(Locale.getDefault(), " %s %d", description, sensitivity.getSensitivity()));
            sensitivitySeekBar.setMin(Controls.SensitivitySetting.MIN);
            sensitivitySeekBar.setMax(Controls.SensitivitySetting.MAX);
            sensitivitySeekBar.setProgress(sensitivity.getSensitivity());
            sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sensitivityDescription.setText(String.format(Locale.getDefault(), " %s %d", description, progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    sensitivity.setSensitivity(seekBar.getProgress());
                }
            });
            generalSettingContainer.addView(sensitivitySetting);
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
