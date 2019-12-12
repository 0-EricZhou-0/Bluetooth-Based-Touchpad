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
    private static HashMap<String, TaskDetail> taskDetail = new HashMap<>();
    private static HashMap<String, String> taskToReadableTask = new HashMap<>();
    private static SparseArray<TaskDetail> actionToTask = new SparseArray<>();
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    static TaskDetail getDetail(String taskType) {
        return taskDetail.get(taskType);
    }

    static String getReadableTask(String taskType) {
        return taskToReadableTask.get(taskType);
    }

    static Set<String> getAllOuterControls() {
        return taskToReadableTask.keySet();
    }

    static class TaskDetail {
        static TaskDetail correspondsTo(String outerControl) {
            return taskDetail.get(outerControl);
        }

        private String outerControl;
        private boolean canBeRepeated;

        TaskDetail(String outer, boolean repeat) {
            outerControl = outer;
            canBeRepeated = repeat;
        }

        TaskDetail duplicate() {
            return new TaskDetail(outerControl, canBeRepeated);
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

    static CoordinatePair phoneScreenSize;
    static CoordinatePair targetScreenSize;

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
    private static final TaskDetail CLICK = new TaskDetail("C", false);
    private static final TaskDetail RIGHT_CLICK = new TaskDetail("R", false);
    private static final TaskDetail MOVE_CURSOR = new TaskDetail("M", true);
    private static final TaskDetail SELECT = new TaskDetail("S", false);
    private static final TaskDetail SCROLL = new TaskDetail("L", true);
    private static final TaskDetail RETURN_TO_DESKTOP = new TaskDetail("D", false);
    private static final TaskDetail ENABLE_TASK_MODE = new TaskDetail("T", false);
    private static final TaskDetail SWITCH_APPLICATION = new TaskDetail("A", true);
    private static final TaskDetail SWITCH_TAB = new TaskDetail("F", true);
    private static final TaskDetail UNDO = new TaskDetail("B", false);
    private static final TaskDetail COPY = new TaskDetail("O", false);
    private static final TaskDetail PASTE = new TaskDetail("P", false);
    private static final TaskDetail CUT = new TaskDetail("Q", false);
    private static final TaskDetail DOUBLE_CLICK = new TaskDetail("G", false);
    // This list can be extended
    // DO NOT FORGET TO ADD DESCRIPTION BELOW!!!

    // These two will not be reached by the identifyAndSend method
    private static final TaskDetail ACTION_EXITING_TOUCH_PAD = new TaskDetail("I", false);
    private static final TaskDetail ACTION_ENTERING_SETTING = new TaskDetail("E", false);

    // Functional controls
    private static final TaskDetail CANCEL_LAST_ACTION = new TaskDetail("N", true);
    private static final TaskDetail HEARTBEAT = new TaskDetail("H", true);
    static final TaskDetail ACTION_NOT_FOUND = new TaskDetail("W", false);


    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    private static SparseBooleanArray settings = new SparseBooleanArray();
    private static SparseArray<String> settingsButtonDescription = new SparseArray<>();


    private static void addControl(TaskDetail detail, String description) {
        taskDetail.put(detail.getOuterControl(), detail);
        taskToReadableTask.put(detail.getOuterControl(), description);
    }
    static void init(Context setContext) {
        context = setContext;


        // General settings (to be refactored)
        settingsButtonDescription.put('O', "TOUCH PAD ORIENTATION");
        settingsButtonDescription.put('S', "SCROLL MODE");
        settingsButtonDescription.put('T', "TOUCH WARNING");
        settingsButtonDescription.put('C', "CURSOR MODE");

        // Adding description of each action can be done
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
        // This list can be extended
        // DO NOT FORGET TO ADD DESCRIPTION HERE!!! Otherwise the newly added control will not work.
        // If the description contains "Basic", it will not be allowed to be modified in the settings

        // These two will not be reached by the identifyAndSend method
        addControl(ACTION_EXITING_TOUCH_PAD, "Exiting Touch Pad (Basic Control)");
        addControl(ACTION_ENTERING_SETTING, "Entering Setting (Basic Control)");

        // Functional controls
        taskDetail.put(CANCEL_LAST_ACTION.getOuterControl(), CANCEL_LAST_ACTION);
        taskDetail.put(HEARTBEAT.getOuterControl(), HEARTBEAT);
        taskDetail.put(ACTION_NOT_FOUND.getOuterControl(), ACTION_NOT_FOUND);

        try {
            // Try to load the json file
            loadJsonFile();
        } catch (FileNotFoundException e) {
            // If the file does not exist
            // The default actionToTask
            actionToTask.append(SINGLE_FINGER + TAP, CLICK);
            actionToTask.append(SINGLE_FINGER + DOUBLE_TAP, DOUBLE_CLICK);
            actionToTask.append(TWO_FINGERS + TAP, RIGHT_CLICK);
            actionToTask.append(SINGLE_FINGER + MOVE, MOVE_CURSOR);
            actionToTask.append(SINGLE_FINGER + LONG_PRESS, SELECT);
            actionToTask.append(TWO_FINGERS + MOVE, SCROLL);
            actionToTask.append(THREE_FINGERS + MOVE_DOWN, RETURN_TO_DESKTOP);
            actionToTask.append(THREE_FINGERS + MOVE_UP, ENABLE_TASK_MODE);
            actionToTask.append(THREE_FINGERS + MOVE_LEFT, SWITCH_APPLICATION);
            actionToTask.append(THREE_FINGERS + MOVE_RIGHT, SWITCH_APPLICATION);
            actionToTask.append(FOUR_FINGERS + MOVE_UP, ACTION_EXITING_TOUCH_PAD);
            actionToTask.append(FOUR_FINGERS + MOVE_DOWN, ACTION_ENTERING_SETTING);

            actionToTask.append(MOVE_CANCEL, CANCEL_LAST_ACTION);
            actionToTask.append(HEARTBEAT_ACTION, HEARTBEAT);

            // Adding general settings (to be refactored)
            settings.put('O', true);     // Orientation
            settings.put('S', true);     // Scroll Mode
            settings.put('T', true);     // Touch Warning
            settings.put('C', true);     // Cursor Mode

            // Save the default actionToTask file
            saveJsonFile();
        }
    }

    static SparseArray<TaskDetail> getCurrentMapping() {
        SparseArray<TaskDetail> toReturn = new SparseArray<>();
        for (int i = 0; i < actionToTask.size(); i++) {
            TaskDetail detail = actionToTask.valueAt(i);
            toReturn.put(actionToTask.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    static void remapping(SparseArray<TaskDetail> newMapping) {
        actionToTask = newMapping;
        PermanentConnection.TouchEventMappingControl.updateMapping();
        saveJsonFile();
    }

    private static void loadJsonFile() throws FileNotFoundException {
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
            throw new FileNotFoundException();
        }
        if (stringBuilder.toString().equals("")) {
            throw new FileNotFoundException();
        }

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(stringBuilder.toString(), JsonObject.class);
        if (jsonObject == null) {
            throw new FileNotFoundException();
        }
        JsonArray mappingControls = (JsonArray) jsonObject.get("mappingControls");

        actionToTask.clear();
        for (JsonElement o : mappingControls) {
            JsonObject individualMapping = (JsonObject) o;
            byte combinedAction = individualMapping.get("combinedAction").getAsByte();
            String outerControl = individualMapping.get("outerControl").getAsString();
            actionToTask.put(combinedAction, getDetail(outerControl));
        }

        settings.clear();
        JsonArray generalSettings = (JsonArray) jsonObject.get("generalSettings");
        for (JsonElement o : generalSettings) {
            JsonObject individualSetting = (JsonObject) o;
            int setting = individualSetting.get("individualSetting").getAsInt();
            boolean status = individualSetting.get("status").getAsBoolean();
            settings.put(setting, status);
        }
    }

    private static void saveJsonFile() {
        JsonObject toSave = new JsonObject();

        JsonArray mappingControls = new JsonArray();
        for (int i = 0; i < actionToTask.size(); i++) {
            JsonObject individualMapping = new JsonObject();
            TaskDetail detail = Controls.getDetail(actionToTask.valueAt(i).getOuterControl());
            byte combinedAction = (byte) actionToTask.keyAt(i);
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

    static String getReadableDefinedAction(byte combinedAction, TaskDetail detail) {
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
        return toReturn + " --- " + taskToReadableTask.get(detail.getOuterControl());
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
            case 'C':
                if (status) {
                    return "RELATIVE";
                }
                return "ABSOLUTE";
            default:
                return "FALSE ARGUMENT";
        }
    }
}
