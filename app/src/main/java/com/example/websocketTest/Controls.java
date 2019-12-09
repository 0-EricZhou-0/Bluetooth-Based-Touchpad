package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;

class Controls {
    private static HashMap<String, OutSettingDetail> taskDetail = new HashMap<>();
    private static HashMap<String, String> readableAction = new HashMap<>();
    private static SparseArray<OutSettingDetail> mapping = new SparseArray<>();
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    static OutSettingDetail getDetail(String taskType) {
        return taskDetail.get(taskType);
    }

    static String getReadableAction(String action) {
        return readableAction.get(action);
    }

    static Set<String> getAllActions() {
        return readableAction.keySet();
    }

    static class OutSettingDetail {
        static OutSettingDetail getSetting(String outerControl) {
            return taskDetail.get(outerControl);
        }

        private String outerControl;
        private boolean canBeRepeated;

        OutSettingDetail(String outer, boolean repeat) {
            outerControl = outer;
            canBeRepeated = repeat;
        }

        OutSettingDetail duplicate() {
            return new OutSettingDetail(outerControl, canBeRepeated);
        }

        String getOuterControl() {
            return outerControl;
        }

        boolean getCanBeRepeated() {
            return canBeRepeated;
        }

        // Warning, this method should not be used to modify the final variables.
        void setCanBeRepeated(boolean repeat) {
            canBeRepeated = repeat;
        }
    }

    // Inner Controls
    static final byte TAP = 0b000;                  //0
    static final byte MOVE = 0b001;                 //1
    static final byte LONG_PRESS = 0b010;           //2
    static final byte MOVE_LEFT = 0b011;            //3
    static final byte MOVE_RIGHT = 0b100;           //4
    static final byte MOVE_UP = 0b101;              //5
    static final byte MOVE_DOWN = 0b110;            //6
    static final byte DOUBLE_TAP = 0b111;           //7

    static final byte SINGLE_FINGER = 0b1000;       //8
    static final byte TWO_FINGERS = 0b10000;        //16
    static final byte THREE_FINGERS = 0b11000;      //24
    static final byte FOUR_FINGERS = 0b100000;      //32
    static final byte FIVE_FINGERS = 0b101000;      //40
    static final byte SIX_FINGERS = 0b110000;       //48 Cannot be accessed by setting
    static final byte SEVEN_FINGERS = 0b111000;     //56 Cannot be accessed by setting
    static final byte EIGHT_FINGERS = 0b1000000;    //64 Cannot be accessed by setting
    static final byte NINE_FINGERS = 0b1001000;     //72 Cannot be accessed by setting
    static final byte TEN_FINGERS = 0b1010000;      //80 Cannot be accessed by setting
    static final byte HEARTBEAT_ACTION = 0b1011000; //88 Cannot be accessed by setting
    static final byte MOVE_CANCEL = 0b1100000;     //96 Cannot be accessed by setting

    //Outer Controls
    private static final OutSettingDetail CLICK = new OutSettingDetail("C", false);
    private static final OutSettingDetail RIGHT_CLICK = new OutSettingDetail("R", false);
    private static final OutSettingDetail MOVE_CURSOR = new OutSettingDetail("M", true);
    private static final OutSettingDetail SELECT = new OutSettingDetail("S", false);
    private static final OutSettingDetail SCROLL = new OutSettingDetail("L", true);
    private static final OutSettingDetail RETURN_TO_DESKTOP = new OutSettingDetail("D", false);
    private static final OutSettingDetail ENABLE_TASK_MODE = new OutSettingDetail("T", false);
    private static final OutSettingDetail SWITCH_APPLICATION = new OutSettingDetail("A", true);
    private static final OutSettingDetail SWITCH_TAB = new OutSettingDetail("F", true);
    private static final OutSettingDetail UNDO = new OutSettingDetail("B", false);
    private static final OutSettingDetail COPY = new OutSettingDetail("O", false);
    private static final OutSettingDetail PASTE = new OutSettingDetail("P", false);
    private static final OutSettingDetail CUT = new OutSettingDetail("Q", false);
    private static final OutSettingDetail DOUBLE_CLICK = new OutSettingDetail("G", false);

    static final OutSettingDetail ACTION_NOT_FOUND = new OutSettingDetail("W", false);
    private static final OutSettingDetail ACTION_EXITING_TOUCH_PAD = new OutSettingDetail("I", false);
    private static final OutSettingDetail ACTION_ENTERING_SETTING = new OutSettingDetail("E", false);
    private static final OutSettingDetail CANCEL_LAST_ACTION = new OutSettingDetail("N", true);
    private static final OutSettingDetail HEARTBEAT = new OutSettingDetail("H", true);


    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    private static SparseBooleanArray settings = new SparseBooleanArray();
    private static SparseArray<String> settingsButtonDescription = new SparseArray<>();


    private static void addControl() {

    }
    static void init(Context setContext) {
        context = setContext;

        settingsButtonDescription.put('O', "TOUCH PAD ORIENTATION");
        settingsButtonDescription.put('S', "SCROLL MODE");
        settingsButtonDescription.put('T', "TOUCH WARNING");

        taskDetail.put(CLICK.getOuterControl(), CLICK);
        readableAction.put(CLICK.getOuterControl(), "Click (Basic Control)");
        taskDetail.put(RIGHT_CLICK.getOuterControl(), RIGHT_CLICK);
        readableAction.put(RIGHT_CLICK.getOuterControl(), "Right Click (Basic Control)");
        taskDetail.put(MOVE_CURSOR.getOuterControl(), MOVE_CURSOR);
        readableAction.put(MOVE_CURSOR.getOuterControl(), "Move Cursor (Basic Control)");
        taskDetail.put(SELECT.getOuterControl(), SELECT);
        readableAction.put(SELECT.getOuterControl(), "Select (Basic Control)");
        taskDetail.put(SCROLL.getOuterControl(), SCROLL);
        readableAction.put(SCROLL.getOuterControl(), "Scroll (Basic Control)");
        taskDetail.put(RETURN_TO_DESKTOP.getOuterControl(), RETURN_TO_DESKTOP);
        readableAction.put(RETURN_TO_DESKTOP.getOuterControl(), "Return to Desktop");
        taskDetail.put(ENABLE_TASK_MODE.getOuterControl(), ENABLE_TASK_MODE);
        readableAction.put(ENABLE_TASK_MODE.getOuterControl(), "Enable Task Mode");
        taskDetail.put(SWITCH_APPLICATION.getOuterControl(), SWITCH_APPLICATION);
        readableAction.put(SWITCH_APPLICATION.getOuterControl(), "Switch Application");
        taskDetail.put(SWITCH_TAB.getOuterControl(), SWITCH_TAB);
        readableAction.put(SWITCH_TAB.getOuterControl(), "Switch Tab");
        taskDetail.put(UNDO.getOuterControl(), UNDO);
        readableAction.put(UNDO.getOuterControl(), "Undo");
        taskDetail.put(COPY.getOuterControl(), COPY);
        readableAction.put(COPY.getOuterControl(), "Copy");
        taskDetail.put(PASTE.getOuterControl(), PASTE);
        readableAction.put(PASTE.getOuterControl(), "Paste");
        taskDetail.put(CUT.getOuterControl(), CUT);
        readableAction.put(CUT.getOuterControl(), "Cut");
        taskDetail.put(DOUBLE_CLICK.getOuterControl(), DOUBLE_CLICK);
        readableAction.put(DOUBLE_CLICK.getOuterControl(), "Double Click (Basic Control)");

        taskDetail.put(ACTION_NOT_FOUND.getOuterControl(), ACTION_NOT_FOUND);
        taskDetail.put(ACTION_EXITING_TOUCH_PAD.getOuterControl(), ACTION_EXITING_TOUCH_PAD);
        readableAction.put(ACTION_EXITING_TOUCH_PAD.getOuterControl(), "Exiting Touch Pad (Basic Control)");
        taskDetail.put(ACTION_ENTERING_SETTING.getOuterControl(), ACTION_ENTERING_SETTING);
        readableAction.put(ACTION_ENTERING_SETTING.getOuterControl(), "Entering Setting (Basic Control)");
        taskDetail.put(CANCEL_LAST_ACTION.getOuterControl(), CANCEL_LAST_ACTION);
        taskDetail.put(HEARTBEAT.getOuterControl(), HEARTBEAT);

        try {
            mapping = loadJsonFile();
            // Toast.makeText(context, mapping.size() + "", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            mapping.append(SINGLE_FINGER + TAP, CLICK);
            mapping.append(SINGLE_FINGER + DOUBLE_TAP, DOUBLE_CLICK);
            mapping.append(TWO_FINGERS + TAP, RIGHT_CLICK);
            mapping.append(SINGLE_FINGER + MOVE, MOVE_CURSOR);
            mapping.append(SINGLE_FINGER + LONG_PRESS, SELECT);
            mapping.append(TWO_FINGERS + MOVE, SCROLL);
            mapping.append(THREE_FINGERS + MOVE_DOWN, RETURN_TO_DESKTOP);
            mapping.append(THREE_FINGERS + MOVE_UP, ENABLE_TASK_MODE);
            mapping.append(THREE_FINGERS + MOVE_LEFT, SWITCH_APPLICATION);
            mapping.append(THREE_FINGERS + MOVE_RIGHT, SWITCH_APPLICATION);
            mapping.append(FOUR_FINGERS + MOVE_UP, ACTION_EXITING_TOUCH_PAD);
            mapping.append(FOUR_FINGERS + MOVE_DOWN, ACTION_ENTERING_SETTING);

            mapping.append(MOVE_CANCEL, CANCEL_LAST_ACTION);
            mapping.append(HEARTBEAT_ACTION, HEARTBEAT);

            settings.put('O', true);     // Orientation
            settings.put('S', true);     // Scroll Mode
            settings.put('T', true);     // Touch Warning
            saveJsonFile();
        }
    }

    static SparseArray<OutSettingDetail> getCurrentMapping() {
        SparseArray<OutSettingDetail> toReturn = new SparseArray<>();
        for (int i = 0; i < mapping.size(); i++) {
            OutSettingDetail detail = mapping.valueAt(i);
            toReturn.put(mapping.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    static void remapping(SparseArray<Controls.OutSettingDetail> newMapping) {
        mapping = newMapping;
        PermanentConnection.TouchEventMappingControl.init();
        saveJsonFile();
    }

    private static SparseArray<OutSettingDetail> loadJsonFile() throws FileNotFoundException {
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader inputStreamReader = new InputStreamReader(context.openFileInput("mappingDetails.json"), StandardCharsets.UTF_8);
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            // Error occurred when opening raw file for reading.
            // Toast.makeText(context, "READ ERROR", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (stringBuilder.toString().equals("")) {
            // Toast.makeText(context, "EMPTY FILE", Toast.LENGTH_SHORT).show();
            return null;
        }

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(stringBuilder.toString(), JsonObject.class);
        if (jsonObject == null) {
            return null;
        }
        JsonArray mappingControls = (JsonArray) jsonObject.get("mappingControls");
        SparseArray<OutSettingDetail> toReturn = new SparseArray<>();
        for (JsonElement o : mappingControls) {
            JsonObject individualMapping = (JsonObject) o;
            byte combinedAction = individualMapping.get("combinedAction").getAsByte();
            String outerControl = individualMapping.get("outerControl").getAsString();
            toReturn.put(combinedAction, getDetail(outerControl));
        }

        JsonArray generalSettings = (JsonArray) jsonObject.get("generalSettings");
        for (JsonElement o : generalSettings) {
            JsonObject individualSetting = (JsonObject) o;
            int setting = individualSetting.get("individualSetting").getAsInt();
            boolean status = individualSetting.get("status").getAsBoolean();
            settings.put(setting, status);
        }

        return toReturn;
    }

    private static void saveJsonFile() {
        JsonObject toSave = new JsonObject();

        JsonArray mappingControls = new JsonArray();
        for (int i = 0; i < mapping.size(); i++) {
            JsonObject individualMapping = new JsonObject();
            Controls.OutSettingDetail detail = Controls.getDetail(mapping.valueAt(i).getOuterControl());
            byte combinedAction = (byte) mapping.keyAt(i);
            String outerControl = detail.getOuterControl();
            individualMapping.addProperty("combinedAction", combinedAction);
            individualMapping.addProperty("outerControl", outerControl);
            mappingControls.add(individualMapping);
        }
        toSave.add("mappingControls", mappingControls);
        JsonArray generalSettings = new JsonArray();
        for (int i = 0; i < settings.size(); i++) {
            JsonObject individualSetting = new JsonObject();
            int setting = settings.keyAt(i);
            boolean status = settings.valueAt(i);
            individualSetting.addProperty("individualSetting", setting);
            individualSetting.addProperty("status", status);
            generalSettings.add(individualSetting);
        }
        toSave.add("generalSettings", generalSettings);

        try {
            FileOutputStream outputStreamWriter = context.openFileOutput("mappingDetails.json", Context.MODE_PRIVATE);
            outputStreamWriter.write(toSave.toString().getBytes());
            outputStreamWriter.close();
        } catch (IOException e) {
            // Toast.makeText(context, "SAVE ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    static void maximumWindow(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    static String getButtonDescription(char setting) {
        return settingsButtonDescription.get(setting);
    }

    static SparseBooleanArray getCurrentSettingStatus() {
        return settings;
    }

    static void setCurrentSettingStatus(SparseBooleanArray newSettings) {
        settings = newSettings;
    }

    static String getReadableDefinedAction(byte combinedAction, OutSettingDetail detail) {
        int numFingers = combinedAction / 8;
        int action = combinedAction % 8;
        if (numFingers > 10) {
            return null;
        }
        String toReturn = "  " + numFingers + " Finger ";
        if (numFingers == 0) {
            toReturn = "";
        }
        switch (action) {
            case TAP:
                toReturn += "Tap";
                break;
            case MOVE:
                toReturn += "Move";
                break;
            case LONG_PRESS:
                toReturn += "Long Press";
                break;
            case MOVE_LEFT:
                toReturn += "Move Left";
                break;
            case MOVE_RIGHT:
                toReturn += "Move Right";
                break;
            case MOVE_UP:
                toReturn += "Move Up";
                break;
            case MOVE_DOWN:
                toReturn += "Move Down";
                break;
        }
        if (numFingers == 0) {
            return toReturn;
        }
        return toReturn + " --- " + readableAction.get(detail.getOuterControl());
    }


    static String getSetting(SparseBooleanArray settingsArray, char setting) {
        boolean status;
        if (settingsArray == null) {
            status = settings.get(setting);
        } else {
            status = settingsArray.get(setting);
        }
        switch (setting) {
            case 'O':
                if (status) {
                    return "VERTICAL";
                }
                return "HORIZONTAL";
            case 'S':
                if (status) {
                    return "FORWARD";
                }
                return "REVERSE";
            case 'T':
                if (status) {
                    return "ENABLED";
                }
                return "DISABLED";
            default:
                return "FALSE ARGUMENT";
        }
    }
}
