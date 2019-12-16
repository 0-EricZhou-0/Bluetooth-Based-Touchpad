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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

class Controls {

    /**
     * Mapping task to its own TaskDetail.
     */
    private static HashMap<String, TaskDetail> taskDetail = new HashMap<>();

    /**
     * To be refactored, will be mapping from String which describes the setting to Boolean which describes its current setting
     */
    private static SparseBooleanArray generalSettings = new SparseBooleanArray();

    /**
     * To be refactored, will be combined to generalSetting detail
     */
    private static SparseArray<String> settingsButtonDescription = new SparseArray<>();

    /**
     * Mapping action (inner control) to task (outer control).
     */
    private static SparseArray<TaskDetail> actionToTask = new SparseArray<>();
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    /**
     * Get the readable description corresponds to the task.
     *
     * @param taskType The String representing the task.
     * @return String which describes the function of the task.
     */
    static String getReadableTask(String taskType) {
        return Objects.requireNonNull(taskDetail.get(taskType)).getDescription();
    }

    /**
     * Return the representation String of all tasks  supported now, and which descriptions are not null.
     * Description is null indicates that the action is a functional action, which user does not need to see.
     *
     * @return A set of Strings representing the task.
     */
    static Set<String> getAllTasks() {
        Set<String> toReturn = new TreeSet<>();
        for (String s : taskDetail.keySet()) {
            if (Objects.requireNonNull(taskDetail.get(s)).getDescription() != null) {
                toReturn.add(s);
            }
        }
        return toReturn;
    }

    /**
     * The class storing the description of every task.
     */
    static class TaskDetail {
        static TaskDetail correspondsTo(String outerControl) {
            return taskDetail.get(outerControl);
        }

        private String task;
        private String description;
        private boolean canBeRepeated;

        TaskDetail(String setTask, String setDescription, boolean repeat) {
            task = setTask;
            description = setDescription;
            canBeRepeated = repeat;
        }

        TaskDetail duplicate() {
            return new TaskDetail(task, description, canBeRepeated);
        }

        String getTask() {
            return task;
        }

        String getDescription() {
            return description;
        }

        boolean getCanBeRepeated() {
            return canBeRepeated;
        }

        TaskDetail add() {
            taskDetail.put(task, this);
            return this;
        }

        // Warning, this method should not be used to modify the final variables.
        void setCanBeRepeated(boolean repeat) {
            canBeRepeated = repeat;
        }
    }

    static CoordinatePair phoneScreenSize;

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

    // Functional controls
    private static final TaskDetail CANCEL_LAST_ACTION = new TaskDetail("N", null, true);
    private static final TaskDetail HEARTBEAT = new TaskDetail("H", null, true);
    static final TaskDetail ACTION_NOT_FOUND = new TaskDetail("W", null, false);


    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    private static void addMapping(TaskDetail... details) {
        for (TaskDetail detail : details) {
            detail.add();
        }
    }

    static void init(Context setContext) {
        context = setContext;

        //Outer Controls
        TaskDetail CLICK = new TaskDetail("C", "Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail RIGHT_CLICK = new TaskDetail("R", "Right Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail DOUBLE_CLICK = new TaskDetail("G", "Double Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail MOVE_CURSOR_RELATIVE = new TaskDetail("M", "Move Cursor (Relative) " + context.getString(R.string.basicControl), true).add();
        TaskDetail MOVE_CURSOR_ABSOLUTE = new TaskDetail("J", "Move Cursor (Absolute) " + context.getString(R.string.basicControl), true).add();
        TaskDetail SELECT = new TaskDetail("S", "Select " + context.getString(R.string.basicControl), false).add();
        TaskDetail SCROLL = new TaskDetail("L", "Scroll " + context.getString(R.string.basicControl), true).add();
        TaskDetail RETURN_TO_DESKTOP = new TaskDetail("D", "Return to Desktop", false).add();
        TaskDetail ENABLE_TASK_MODE = new TaskDetail("T", "Enable Task Mode", false).add();
        TaskDetail SWITCH_APPLICATION = new TaskDetail("A", "Switch Application", true).add();
        TaskDetail SWITCH_TAB = new TaskDetail("F", "Switch Tab", true).add();
        TaskDetail UNDO = new TaskDetail("B", "Undo", false).add();
        TaskDetail COPY = new TaskDetail("O", "Copy", false).add();
        TaskDetail PASTE = new TaskDetail("P", "Paste", false).add();
        TaskDetail CUT = new TaskDetail("Q", "Cut", false).add();
        /* This list can be extended
        If the description contains R.string.BasicControl, it will not be allowed to be modified in the generalSettings
        The format of any extension format is as follows:
            TaskDetail NAME = new TaskDetail(stringRepresentation, stringDescription, canBeRepeated).add();     */


        // These two will not be reached by the identifyAndSend method
        TaskDetail ACTION_EXITING_TOUCH_PAD = new TaskDetail("I", "Exiting Touch Pad " + context.getString(R.string.basicControl), false).add();
        TaskDetail ACTION_ENTERING_SETTING = new TaskDetail("E", "Entering Setting " + context.getString(R.string.basicControl), false).add();

        // General generalSettings (to be refactored)
        settingsButtonDescription.put('O', "TOUCH PAD ORIENTATION");
        settingsButtonDescription.put('S', "SCROLL MODE");
        settingsButtonDescription.put('T', "TOUCH WARNING");
        settingsButtonDescription.put('C', "CURSOR MODE");

        // These two will not be reached by the identifyAndSend method
        addMapping(ACTION_EXITING_TOUCH_PAD, ACTION_ENTERING_SETTING);

        // Functional controls
        addMapping(CANCEL_LAST_ACTION, HEARTBEAT, ACTION_NOT_FOUND);

        try {
            // Try to load the json file
            loadJsonFile();
        } catch (FileNotFoundException e) {
            // If the file does not exist
            // The default actionToTask
            actionToTask.append(SINGLE_FINGER + TAP, CLICK);
            actionToTask.append(SINGLE_FINGER + DOUBLE_TAP, DOUBLE_CLICK);
            actionToTask.append(TWO_FINGERS + TAP, RIGHT_CLICK);
            actionToTask.append(SINGLE_FINGER + MOVE, MOVE_CURSOR_RELATIVE);
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

            // Adding general generalSettings (to be refactored)
            generalSettings.put('O', true);     // Orientation
            generalSettings.put('S', true);     // Scroll Mode
            generalSettings.put('T', true);     // Touch Warning
            generalSettings.put('C', true);     // Cursor Mode

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

    static void changeCursorMoveMode(boolean isRelative) {
        if (isRelative) {
            actionToTask.put(SINGLE_FINGER + MOVE, taskDetail.get("M"));
        } else {
            actionToTask.put(SINGLE_FINGER + MOVE, taskDetail.get("J"));
        }
        PermanentConnection.TouchEventMappingControl.updateMapping();
        saveJsonFile();
    }

    static void resetting(SparseBooleanArray newGeneralSetting) {
        generalSettings = newGeneralSetting;
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
            String outerControl = individualMapping.get("task").getAsString();
            actionToTask.put(combinedAction, taskDetail.get(outerControl));
        }

        generalSettings.clear();
        JsonArray generalSettings = (JsonArray) jsonObject.get("generalSettings");
        for (JsonElement o : generalSettings) {
            JsonObject individualSetting = (JsonObject) o;
            int setting = individualSetting.get("individualSetting").getAsInt();
            boolean status = individualSetting.get("status").getAsBoolean();
            Controls.generalSettings.put(setting, status);
        }
    }

    private static void saveJsonFile() {
        JsonObject toSave = new JsonObject();

        JsonArray mappingControls = new JsonArray();
        for (int i = 0; i < actionToTask.size(); i++) {
            JsonObject individualMapping = new JsonObject();
            TaskDetail detail = taskDetail.get(actionToTask.valueAt(i).getTask());
            byte combinedAction = (byte) actionToTask.keyAt(i);
            assert detail != null;
            String outerControl = detail.getTask();
            individualMapping.addProperty("combinedAction", combinedAction);
            individualMapping.addProperty("task", outerControl);
            mappingControls.add(individualMapping);
        }
        toSave.add("mappingControls", mappingControls);
        JsonArray generalSettings = new JsonArray();
        for (int i = 0; i < Controls.generalSettings.size(); i++) {
            JsonObject individualSetting = new JsonObject();
            int setting = Controls.generalSettings.keyAt(i);
            boolean status = Controls.generalSettings.valueAt(i);
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
        return generalSettings;
    }

    static void setCurrentSettingStatus(SparseBooleanArray newSettings) {
        generalSettings = newSettings;
    }

    static String getReadableDefinedAction(byte combinedAction, TaskDetail detail) {
        int numFingers = combinedAction / 8;
        int action = combinedAction % 8;
        if (numFingers > 10) {
            return null;
        }
        String toReturn;
        if (numFingers == 0) {
            toReturn = "";
        } else {
            toReturn = String.format(Locale.getDefault(), "  %d %s ", numFingers, context.getString(R.string.finger));
        }
        switch (action) {
            case TAP:
                toReturn += context.getString(R.string.tap);
                break;
            case MOVE:
                toReturn += context.getString(R.string.move);
                break;
            case LONG_PRESS:
                toReturn += context.getString(R.string.longPress);
                break;
            case MOVE_LEFT:
                toReturn += context.getString(R.string.moveLeft);
                break;
            case MOVE_RIGHT:
                toReturn += context.getString(R.string.moveRight);
                break;
            case MOVE_UP:
                toReturn += context.getString(R.string.moveUp);
                break;
            case MOVE_DOWN:
                toReturn += context.getString(R.string.moveDown);
                break;
        }
        if (numFingers == 0) {
            return toReturn;
        }
        return toReturn + " --- " + detail.getDescription();
    }


    static String getSetting(SparseBooleanArray settingsArray, char setting) {
        boolean status;
        if (settingsArray == null) {
            status = generalSettings.get(setting);
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
