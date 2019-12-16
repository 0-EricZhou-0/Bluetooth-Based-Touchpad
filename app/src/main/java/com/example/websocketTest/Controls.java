package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.TextView;

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
import java.util.ArrayList;
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
     * ArrayList storing all the settings.
     */
    private static ArrayList<SettingDetail> settingDetail = new ArrayList<>();

    /**
     * To be refactored, will be mapping from string which describes the setting to Boolean which describes its current setting
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
     * @param taskType the string representing the task
     * @return string which describes the function of the task
     */
    static String getReadableTask(String taskType) {
        return Objects.requireNonNull(taskDetail.get(taskType)).getDescription();
    }

    /**
     * Return the representation string of all tasks  supported now, and which descriptions are not null.
     * Description is null indicates that the action is a functional action, which user does not need to see.
     *
     * @return a set of Strings representing the task
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

        /**
         * string representation of task.
         */
        private String task;

        /**
         * Readable description of the task.
         */
        private String description;

        /**
         * Can the task be repeated.
         * For example, switching applications can be repeated, since it can be repeated under one
         * complete cycle of touch (from touch to release). However, return to desktop cannot be
         * repeated, since under each cycle the task can be only performed once.
         */
        private boolean canBeRepeated;

        /**
         * Constructing a new detail using the representation string, description and a boolean describing
         * can it be repeated.
         *
         * @param setTask        task representation string
         * @param setDescription description of the task
         * @param repeat         can the task be repeated
         */
        TaskDetail(String setTask, String setDescription, boolean repeat) {
            task = setTask;
            description = setDescription;
            canBeRepeated = repeat;
        }

        /**
         * Get a duplication of the detail.
         *
         * @return a duplication of the detail
         */
        TaskDetail duplicate() {
            return new TaskDetail(task, description, canBeRepeated);
        }

        /**
         * Get the representation string of the task.
         *
         * @return representation string of the task
         */
        String getTask() {
            return task;
        }

        /**
         * Get the description of the task.
         *
         * @return description of the task
         */
        String getDescription() {
            return description;
        }

        /**
         * Get a boolean representing if the action can be repeated.
         *
         * @return can the action be repeated
         */
        boolean getCanBeRepeated() {
            return canBeRepeated;
        }

        /**
         * Adding the detail to the mapping for it to be functional, used when initialization
         *
         * @return the detail itself
         */
        TaskDetail add() {
            taskDetail.put(task, this);
            return this;
        }

        /**
         * Used when running touch pad, once the boolean representation is modified to false, it indicates
         * the task cannot be done again until the user starts another cycle of touch event.
         * <p>
         * Warning, this method should not be used to modify the ORIGINAL detail, all the modifications
         * should be done on a duplication of the detail.
         *
         * @param repeat can the action be repeated
         */
        void setCanBeRepeated(boolean repeat) {
            canBeRepeated = repeat;
        }
    }

    /**
     * The class storing the description of every general setting.
     */
    static class SettingDetail {
        private String settingDescription;
        private String detailedDescription;
        private int currentIdx;
        private int[] allStates;

        SettingDetail(String setSettingDescription, String setDetailedDescription, int setCurrentIdx,
                      int... setAllStates) {
            if (setCurrentIdx > setAllStates.length) {
                throw new IllegalArgumentException(String.format(
                        "Current index cannot exceed number of states. Try to access %d. But only have %d states.",
                        setCurrentIdx, setAllStates.length));
            }
            settingDescription = setSettingDescription;
            detailedDescription = setDetailedDescription;
            currentIdx = setCurrentIdx;
            allStates = setAllStates;
        }

        void setDescription(TextView description) {
            description.setText(detailedDescription);
        }

        void changeSetting(TextView state, int idx) {
            currentIdx = idx;
            state.setText(String.format("%s : %s", settingDescription, context.getString(allStates[currentIdx])));
        }

        void add() {
            settingDetail.add(this);
        }

    }

    /**
     * The size of the phone screen, used when cursor move state is absolute.
     */
    static CoordinatePair phoneScreenSize;


    /* All the inner control.
    Inner controls responsible for transmitting data inside the application, to send the data, a
    translation os inner control to outer control is required. */
    // Action
    static final byte TAP = 0b000;                  //0
    static final byte MOVE = 0b001;                 //1
    static final byte LONG_PRESS = 0b010;           //2
    static final byte MOVE_LEFT = 0b011;            //3
    static final byte MOVE_RIGHT = 0b100;           //4
    static final byte MOVE_UP = 0b101;              //5
    static final byte MOVE_DOWN = 0b110;            //6
    static final byte DOUBLE_TAP = 0b111;           //7
    // Action Finger Count
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
    /* The Combined Action is the combination of one Action and one Action FingerCount.
    It represents the action that user did on the screen. */

    // Functional outer controls
    private static final TaskDetail CANCEL_LAST_ACTION = new TaskDetail("N", null, true);
    private static final TaskDetail HEARTBEAT = new TaskDetail("H", null, true);
    static final TaskDetail ACTION_NOT_FOUND = new TaskDetail("W", null, false);

    // Coordinate pairs used in comparisons
    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    /**
     * Used when adding functional outer controls to the mapping for them to be functional.
     *
     * @param details all the functional outer controls
     */
    private static void addMapping(TaskDetail... details) {
        for (TaskDetail detail : details) {
            detail.add();
        }
    }

    /**
     * Initialize all the outer controls. If there is a file stored in the device which describes the
     * mapping, read the file for mapping; otherwise, use default mapping.
     */
    static void init(Context setContext) {
        context = setContext;

        /* All the outer control.
        Outer controls are responsible for transmission and adding new mapping. Each outer control
        corresponds to a combination of inner controls. */
        TaskDetail click = new TaskDetail("C", "Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail rightClick = new TaskDetail("R", "Right Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail doubleClick = new TaskDetail("G", "Double Click " + context.getString(R.string.basicControl), false).add();
        TaskDetail moveCursorRelative = new TaskDetail("M", "Move Cursor (Relative) " + context.getString(R.string.basicControl), true).add();
        TaskDetail moveCursorAbsolute = new TaskDetail("J", "Move Cursor (Absolute) " + context.getString(R.string.basicControl), true).add();
        TaskDetail select = new TaskDetail("S", "Select " + context.getString(R.string.basicControl), false).add();
        TaskDetail scroll = new TaskDetail("L", "Scroll " + context.getString(R.string.basicControl), true).add();
        TaskDetail returnToDesktop = new TaskDetail("D", "Return to Desktop", false).add();
        TaskDetail enableTaskMode = new TaskDetail("T", "Enable Task Mode", false).add();
        TaskDetail switchApplication = new TaskDetail("A", "Switch Application", true).add();
        TaskDetail switchTab = new TaskDetail("F", "Switch Tab", true).add();
        TaskDetail undo = new TaskDetail("B", "Undo", false).add();
        TaskDetail copy = new TaskDetail("O", "Copy", false).add();
        TaskDetail paste = new TaskDetail("P", "Paste", false).add();
        TaskDetail cut = new TaskDetail("Q", "Cut", false).add();
        /* This list can be extended
        If the description contains R.string.BasicControl, it will not be allowed to be modified in the generalSettings
        The format of any extension format is as follows:
            TaskDetail NAME = new TaskDetail(stringRepresentation, stringDescription, canBeRepeated).add();     */


        // These two will not be reached by the identifyAndSend method
        TaskDetail actionExitingTouchPad = new TaskDetail("I", "Exiting Touch Pad " + context.getString(R.string.basicControl), false).add();
        TaskDetail actionEnteringSetting = new TaskDetail("E", "Entering Setting " + context.getString(R.string.basicControl), false).add();

        // Functional inner controls
        addMapping(CANCEL_LAST_ACTION, HEARTBEAT, ACTION_NOT_FOUND);

        new SettingDetail(context.getString(R.string.orientation), context.getString(R.string.orientationDescription),
                0, R.string.vertical, R.string.horizontal).add();
        new SettingDetail(context.getString(R.string.scrollMode), context.getString(R.string.scrollModeDescription),
                0, R.string.forward, R.string.reverse).add();
        new SettingDetail(context.getString(R.string.touchWarning), context.getString(R.string.touchWarningDescription),
                0, R.string.enabled, R.string.disabled).add();
        new SettingDetail(context.getString(R.string.cursorMode), context.getString(R.string.cursorModeDescription),
                0, R.string.relative, R.string.absolute).add();

        // General generalSettings (to be refactored)
        settingsButtonDescription.put('O', "TOUCH PAD ORIENTATION");
        settingsButtonDescription.put('S', "SCROLL MODE");
        settingsButtonDescription.put('T', "TOUCH WARNING");
        settingsButtonDescription.put('C', "CURSOR MODE");


        try {
            // Try to load the json file
            loadJsonFile();
        } catch (FileNotFoundException e) {
            // If the file does not exist
            // The default mapping
            actionToTask.append(SINGLE_FINGER + TAP, click);
            actionToTask.append(SINGLE_FINGER + DOUBLE_TAP, doubleClick);
            actionToTask.append(TWO_FINGERS + TAP, rightClick);
            actionToTask.append(SINGLE_FINGER + MOVE, moveCursorRelative);
            actionToTask.append(SINGLE_FINGER + LONG_PRESS, select);
            actionToTask.append(TWO_FINGERS + MOVE, scroll);
            actionToTask.append(THREE_FINGERS + MOVE_DOWN, returnToDesktop);
            actionToTask.append(THREE_FINGERS + MOVE_UP, enableTaskMode);
            actionToTask.append(THREE_FINGERS + MOVE_LEFT, switchApplication);
            actionToTask.append(THREE_FINGERS + MOVE_RIGHT, switchApplication);
            actionToTask.append(FOUR_FINGERS + MOVE_UP, actionExitingTouchPad);
            actionToTask.append(FOUR_FINGERS + MOVE_DOWN, actionEnteringSetting);

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

    /**
     * Get the current mapping from action to task. All functional outer controls are excluded.
     *
     * @return current mapping
     */
    static SparseArray<TaskDetail> getCurrentMapping() {
        SparseArray<TaskDetail> toReturn = new SparseArray<>();
        for (int i = 0; i < actionToTask.size(); i++) {
            TaskDetail detail = actionToTask.valueAt(i);
            toReturn.put(actionToTask.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    /**
     * Change current mapping.
     *
     * @param newMapping new mapping SparseArray.
     */
    static void remapping(SparseArray<TaskDetail> newMapping) {
        actionToTask = newMapping;
        PermanentConnection.TouchEventMappingControl.updateMapping();
        saveJsonFile();
    }

    /**
     * Change current cursor move mode to specified mode.
     *
     * @param isRelative boolean value states whether cursor move state is relative or not
     */
    static void changeCursorMoveMode(boolean isRelative) {
        if (isRelative) {
            actionToTask.put(SINGLE_FINGER + MOVE, taskDetail.get("M"));
        } else {
            actionToTask.put(SINGLE_FINGER + MOVE, taskDetail.get("J"));
        }
        PermanentConnection.TouchEventMappingControl.updateMapping();
        saveJsonFile();
    }

    /**
     * Change current general setting.
     * (To be implemented)
     *
     * @param newGeneralSetting new general setting ArrayList
     */
    static void resetting(SparseBooleanArray newGeneralSetting) {
        generalSettings = newGeneralSetting;
    }

    /**
     * Load the settings from local json file "settingDetails.json".
     * <p>
     * After loading, actionToTask and generalSettings will be modified according to the file.
     *
     * @throws FileNotFoundException
     */
    private static void loadJsonFile() throws FileNotFoundException {
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader inputStreamReader = new InputStreamReader(context.openFileInput("settingDetails.json"), StandardCharsets.UTF_8);
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

    /**
     * Save the settings to local json file "settingDetails.json".
     * <p>
     * Mapping info comes from actionToTask and general setting info comes from generalSettings.
     */
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
            FileOutputStream outputStreamWriter = context.openFileOutput("settingDetails.json", Context.MODE_PRIVATE);
            outputStreamWriter.write(toSave.toString().getBytes());
            outputStreamWriter.close();
        } catch (IOException e) {
            // Toast.makeText(context, "SAVE ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set the current window state to be maximized.
     */
    static void maximumWindow(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // To be refactored, will be combined into settingDetail
    static String getButtonDescription(char setting) {
        return settingsButtonDescription.get(setting);
    }

    static SparseBooleanArray getCurrentSettingStatus() {
        return generalSettings;
    }

    static void setCurrentSettingStatus(SparseBooleanArray newSettings) {
        generalSettings = newSettings;
    }

    /**
     * Get description by combined action and a taskDetail instance.
     * <p>
     * If input action instead of combined action, the method will only return the readable description
     * of the action, and the detail is ignored. Otherwise, it will return the full description.
     *
     * @param combinedAction byte representation of specified combined action or action
     * @param detail         corresponding detail
     * @return readable string describing the mapping
     */
    static String getReadableDefinedAction(byte combinedAction, TaskDetail detail) {
        int numFingers = combinedAction / 8;
        int action = combinedAction % 8;
        if (numFingers > 10) {
            throw new IllegalArgumentException("Number of fingers cannot exceed 10, the number of finger requested is " + numFingers);
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
                    return context.getString(R.string.vertical);
                }
                return context.getString(R.string.horizontal);
            case 'S':
                if (status) {
                    return context.getString(R.string.forward);
                }
                return context.getString(R.string.reverse);
            case 'T':
                if (status) {
                    return context.getString(R.string.enabled);
                }
                return context.getString(R.string.disabled);
            case 'C':
                if (status) {
                    return context.getString(R.string.relative);
                }
                return context.getString(R.string.absolute);
            default:
                throw new IllegalArgumentException("This setting does not exist");
        }
    }
}
