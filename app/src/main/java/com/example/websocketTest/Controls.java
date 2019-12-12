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
    private static HashMap<String, OutControlDetail> taskDetail = new HashMap<>();
    private static HashMap<String, String> readableAction = new HashMap<>();
    private static SparseArray<OutControlDetail> mapping = new SparseArray<>();
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    static OutControlDetail getDetail(String taskType) {
        return taskDetail.get(taskType);
    }

    static String getReadableAction(String action) {
        return readableAction.get(action);
    }

    static Set<String> getAllOuterControls() {
        return readableAction.keySet();
    }

    static class OutControlDetail {
        static OutControlDetail correspondsTo(String outerControl) {
            return taskDetail.get(outerControl);
        }

        private String outerControl;
        private boolean canBeRepeated;

        OutControlDetail(String outer, boolean repeat) {
            outerControl = outer;
            canBeRepeated = repeat;
        }

        OutControlDetail duplicate() {
            return new OutControlDetail(outerControl, canBeRepeated);
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
    static final byte SIX_FINGERS = 0b110000;       //48 Not recommended
    static final byte SEVEN_FINGERS = 0b111000;     //56 Not recommended
    static final byte EIGHT_FINGERS = 0b1000000;    //64 Not recommended
    static final byte NINE_FINGERS = 0b1001000;     //72 Not recommended
    static final byte TEN_FINGERS = 0b1010000;      //80 Not recommended
    static final byte HEARTBEAT_ACTION = 0b1011000; //88 Not recommended
    static final byte MOVE_CANCEL = 0b1100000;      //96 Not recommended

    //Outer Controls
    private static final OutControlDetail CLICK = new OutControlDetail("C", false);
    private static final OutControlDetail RIGHT_CLICK = new OutControlDetail("R", false);
    private static final OutControlDetail MOVE_CURSOR = new OutControlDetail("M", true);
    private static final OutControlDetail SELECT = new OutControlDetail("S", false);
    private static final OutControlDetail SCROLL = new OutControlDetail("L", true);
    private static final OutControlDetail RETURN_TO_DESKTOP = new OutControlDetail("D", false);
    private static final OutControlDetail ENABLE_TASK_MODE = new OutControlDetail("T", false);
    private static final OutControlDetail SWITCH_APPLICATION = new OutControlDetail("A", true);
    private static final OutControlDetail SWITCH_TAB = new OutControlDetail("F", true);
    private static final OutControlDetail UNDO = new OutControlDetail("B", false);
    private static final OutControlDetail COPY = new OutControlDetail("O", false);
    private static final OutControlDetail PASTE = new OutControlDetail("P", false);
    private static final OutControlDetail CUT = new OutControlDetail("Q", false);
    private static final OutControlDetail DOUBLE_CLICK = new OutControlDetail("G", false);

    private static final OutControlDetail ACTION_EXITING_TOUCH_PAD = new OutControlDetail("I", false);
    private static final OutControlDetail ACTION_ENTERING_SETTING = new OutControlDetail("E", false);

    private static final OutControlDetail CANCEL_LAST_ACTION = new OutControlDetail("N", true);
    private static final OutControlDetail HEARTBEAT = new OutControlDetail("H", true);
    static final OutControlDetail ACTION_NOT_FOUND = new OutControlDetail("W", false);


    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    private static SparseBooleanArray settings = new SparseBooleanArray();
    private static SparseArray<String> settingsButtonDescription = new SparseArray<>();


    private static void addControl(OutControlDetail detail, String description) {
        taskDetail.put(detail.getOuterControl(), detail);
        readableAction.put(detail.getOuterControl(), description);
    }
    static void init(Context setContext) {
        context = setContext;

        settingsButtonDescription.put('O', "TOUCH PAD ORIENTATION");
        settingsButtonDescription.put('S', "SCROLL MODE");
        settingsButtonDescription.put('T', "TOUCH WARNING");

        addControl(CLICK, "Click (Basic Control)");
        addControl(RIGHT_CLICK, "Right Click (Basic Control)");
        addControl(DOUBLE_CLICK, "Double Click (Basic Control)");
        addControl(MOVE_CURSOR, "Move Cursor (Basic Control)");
        addControl(SELECT, "Select (Basic Control)");
        addControl(SCROLL, "Scroll (Basic Control)");
        addControl(RETURN_TO_DESKTOP, "Return to Desktop");
        addControl(ENABLE_TASK_MODE, "Enable Task Mode");
        addControl(SWITCH_APPLICATION, "Switch Application");
        addControl(SWITCH_TAB, "Switch Tab");
        addControl(UNDO, "Undo");
        addControl(COPY, "Copy");
        addControl(PASTE, "Paste");
        addControl(CUT, "Cut");

        addControl(ACTION_EXITING_TOUCH_PAD, "Exiting Touch Pad (Basic Control)");
        addControl(ACTION_ENTERING_SETTING, "Entering Setting (Basic Control)");

        taskDetail.put(CANCEL_LAST_ACTION.getOuterControl(), CANCEL_LAST_ACTION);
        taskDetail.put(HEARTBEAT.getOuterControl(), HEARTBEAT);
        taskDetail.put(ACTION_NOT_FOUND.getOuterControl(), ACTION_NOT_FOUND);

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

    static SparseArray<OutControlDetail> getCurrentMapping() {
        SparseArray<OutControlDetail> toReturn = new SparseArray<>();
        for (int i = 0; i < mapping.size(); i++) {
            OutControlDetail detail = mapping.valueAt(i);
            toReturn.put(mapping.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    static void remapping(SparseArray<Controls.OutControlDetail> newMapping) {
        mapping = newMapping;
        PermanentConnection.TouchEventMappingControl.updateMapping();
        saveJsonFile();
    }

    private static SparseArray<OutControlDetail> loadJsonFile() throws FileNotFoundException {
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
        SparseArray<OutControlDetail> toReturn = new SparseArray<>();
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
            Controls.OutControlDetail detail = Controls.getDetail(mapping.valueAt(i).getOuterControl());
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

    static String getReadableDefinedAction(byte combinedAction, OutControlDetail detail) {
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
