package com.example.websocketTest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionSettingFragment extends Fragment {

    private View rootView;
    private LinearLayout deviceContainer;
    private List<Controls.DeviceDetail> currentDevices;
    private List<CheckBox> checkBoxList = new ArrayList<>();
    private boolean changeDevicePermitted;

    private void loadDeviceList() {
        deviceContainer.removeAllViews();
        checkBoxList.clear();
        for (final Controls.DeviceDetail detail : currentDevices) {
            final View deviceDetail = getLayoutInflater().inflate(R.layout.chunk_connection_setting,
                    deviceContainer, false);
            final CheckBox selection = deviceDetail.findViewById(R.id.selectDevice);
            final TextView deviceName = deviceDetail.findViewById(R.id.deviceName);
            final TextView deviceMac = deviceDetail.findViewById(R.id.deviceMac);
            final ImageButton deleteDevice = deviceDetail.findViewById(R.id.deleteDevice);
            final LinearLayout selectionPanel = deviceDetail.findViewById(R.id.selectionPanel);
            checkBoxList.add(selection);
            deviceName.setText(detail.getDeviceName());
            deviceMac.setText(detail.getMacAddress());
            deviceName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText rename = new EditText(getContext());
                    rename.setHint(R.string.deviceNameHint);
                    rename.setInputType(InputType.TYPE_CLASS_TEXT);
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.rename)
                            .setMessage(String.format("%s %s", getString(R.string.currentName), detail.getDeviceName()))
                            .setView(rename)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String newName = rename.getText().toString();
                                    if (!newName.equals("")) {
                                        detail.setDeviceName(newName);
                                        deviceName.setText(newName);
                                    }
                                }
                            }).show();
                }
            });
            selection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (CheckBox checkBox : checkBoxList) {
                            checkBox.setChecked(false);
                        }
                        Controls.DeviceDetail.setIndexSelected(checkBoxList.indexOf(selection));
                        selection.setChecked(true);
                    } else {
                        Controls.DeviceDetail.setIndexSelected(-1);
                    }
                }
            });
            deleteDevice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.confirmDeleteDevice)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (selection.isChecked()) {
                                        BluetoothConnection.setServerMac(null);
                                        Controls.DeviceDetail.setIndexSelected(-1);
                                    }
                                    currentDevices.remove(detail);
                                    deviceContainer.removeView(deviceDetail);
                                    checkBoxList.remove(selection);

                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                }
            });
            selectionPanel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selection.setChecked(!selection.isChecked());
                }
            });
            deviceContainer.addView(deviceDetail);
        }
        if (Controls.DeviceDetail.getIndexSelected() != -1) {
            checkBoxList.get(Controls.DeviceDetail.getIndexSelected()).setChecked(true);
        }
    }

    ConnectionSettingFragment(boolean isPermitted) {
        changeDevicePermitted = isPermitted;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        deviceContainer = rootView.findViewById(R.id.definedDeviceList);
        ImageButton addNewDevice = rootView.findViewById(R.id.addNewDevice);
        currentDevices = Controls.getCurrentDevices();
        final LinearLayout deviceInfo = new LinearLayout(getContext());
        deviceInfo.setOrientation(LinearLayout.VERTICAL);
        final EditText deviceName = new EditText(getContext());
        final EditText deviceMac = new EditText(getContext());
        final TextView invalidMac = new TextView(getContext());
        deviceName.setHint(String.format("%s (%s %s)", getString(R.string.deviceNameHint), getString(R.string.setDefault), getString(R.string.defaultDeviceName)));
        deviceName.setInputType(InputType.TYPE_CLASS_TEXT);
        deviceMac.setHint(R.string.deviceMacHint);
        // deviceMac.setText("5CE0C55B28AD");
        deviceName.setTextColor(Color.BLACK);
        deviceMac.setTextColor(Color.BLACK);
        invalidMac.setTextColor(Color.TRANSPARENT);
        deviceMac.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        final AlertDialog addNew = new AlertDialog.Builder(getActivity())
                .setView(deviceInfo)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = deviceName.getText().toString();
                        if (name.equals("")) {
                            name = getString(R.string.defaultDeviceName);
                        }
                        String mac = deviceMac.getText().toString();
                        currentDevices.add(new Controls.DeviceDetail(mac, name));
                        loadDeviceList();
                    }
                })
                .setNegativeButton(R.string.abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        addNew.setCanceledOnTouchOutside(false);
        deviceMac.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String mac = deviceMac.getText().toString();
                if (!hasFocus && !mac.equals("")) {
                    if (Controls.DeviceDetail.isValidMac(mac)) {
                        addNew.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        deviceMac.setTextColor(Color.RED);
                        invalidMac.setText(R.string.invalidMac);
                        invalidMac.setTextColor(Color.RED);
                    }
                }
            }
        });
        deviceMac.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String mac;
                if (s.length() > Controls.MAC_LENGTH) {
                    mac = s.delete(Controls.MAC_LENGTH, s.length()).toString();
                } else {
                    mac = s.toString();
                }
                if (mac.length() == Controls.MAC_LENGTH) {
                    if (Controls.DeviceDetail.isValidMac(mac)) {
                        if (Controls.DeviceDetail.isDuplicated(mac)) {
                            invalidMac.setText(R.string.duplicatedMac);
                            invalidMac.setTextColor(Color.RED);
                        } else {
                            addNew.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    } else {
                        deviceMac.setTextColor(Color.RED);
                        invalidMac.setText(R.string.invalidMac);
                        invalidMac.setTextColor(Color.RED);
                    }
                    s.delete(Controls.MAC_LENGTH, s.length());
                } else {
                    addNew.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    deviceMac.setTextColor(Color.BLACK);
                    invalidMac.setTextColor(Color.TRANSPARENT);
                }
            }
        });
        deviceInfo.addView(deviceName);
        deviceInfo.addView(deviceMac);
        deviceInfo.addView(invalidMac);
        addNewDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNew.show();
                deviceName.setText("");
                deviceMac.setText("");
                invalidMac.setTextColor(Color.TRANSPARENT);
                addNew.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        loadDeviceList();
        final TextView connectionTabDescription = rootView.findViewById(R.id.connectionTabDescription);
        if (!changeDevicePermitted) {
            connectionTabDescription.setText(R.string.tabDisableDescription);
            Controls.setUsability(false, (ViewGroup) rootView);
        } else {
            connectionTabDescription.setText(R.string.connectionSettingDescription);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_connection_setting, container, false);
        return rootView;
    }

}
