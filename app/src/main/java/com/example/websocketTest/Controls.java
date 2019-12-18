package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseArray;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

class Controls {


    static final int ADD_MAPPING = 0;
    static final int MAC_LENGTH = 12;
    /**
     * Mapping task to its own TaskDetail.
     */
    private static HashMap<String, TaskDetail> taskDetail = new HashMap<>();

    /**
     * ArrayList storing all the settings.
     */
    private static ArrayList<SettingDetail> settingDetail = new ArrayList<>();

    /**
     * Mapping action (inner control) to task (outer control).
     */
    private static SparseArray<TaskDetail> actionToTask = new SparseArray<>();

    /**
     * ArrayList storing all the devices entered.
     */
    private static ArrayList<DeviceDetail> deviceDetails = new ArrayList<>();

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

    static int currentSettingTab = 0;

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

        String getDetailedDescription() {
            return detailedDescription;
        }

        String getCurrentState() {
            return context.getString(allStates[currentIdx]);
        }

        String getSettingDescriptionAndState() {
            return String.format("%s : %s", settingDescription, context.getString(allStates[currentIdx]));
        }

        int getCurrentIdx() {
            return currentIdx;
        }

        void setCurrentIdx(int idx) {
            currentIdx = idx;
        }

        String[] getAllStatus() {
            String[] toReturn = new String[allStates.length];
            for (int i = 0; i < toReturn.length; i++) {
                toReturn[i] = context.getString(allStates[i]);
            }
            return toReturn;
        }

        void changeSetting(int idx) {
            currentIdx = idx;
        }

        void add() {
            settingDetail.add(this);
        }

    }

    static class DeviceDetail {
        private static int indexSelected = -1;

        static boolean isValidMac(String mac) {
            if (mac.length() != 12) {
                return false;
            }
            for (int i = 0; i < Controls.MAC_LENGTH; i++) {
                int c = mac.charAt(i);
                if (c < '0' || (c > '9' && c < 'A') || (c > 'F' && c < 'a') || c > 'f') {
                    return false;
                }
            }
            return true;
        }

        static boolean isDuplicated(String mac) {
            for (DeviceDetail detail : deviceDetails) {
                if (detail.macAddress.equals(mac)) {
                    return true;
                }
            }
            return false;
        }

        static void setIndexSelected(int index) {
            indexSelected = index;
        }

        static int getIndexSelected() {
            return indexSelected;
        }

        private String macAddress;
        private String deviceName;

        DeviceDetail(String setMacAddress, String setDeviceName) {
            macAddress = setMacAddress.toUpperCase(Locale.getDefault());
            deviceName = setDeviceName;
        }

        String getRawMac() {
            return macAddress;
        }

        String getMacAddress() {
            return String.format("%s:%s:%s:%s:%s:%s", macAddress.substring(0, 2), macAddress.substring(2, 4),
                    macAddress.substring(4, 6), macAddress.substring(6, 8), macAddress.substring(8, 10), macAddress.substring(10, 12));
        }

        void setDeviceName(String newDeviceName) {
            deviceName = newDeviceName;
        }

        String getDeviceName() {
            return deviceName;
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

    static final int ORIENTATION_SETTING = 0;
    static final int SCROLL_MODE_SETTING = 1;
    static final int TOUCH_WARNING_SETTING = 2;
    static final int CURSOR_MODE_SETTING = 3;

    /**
     * Initialize all the outer controls. If there is a file stored in the device which describes the
     * mapping, read the file for mapping; otherwise, use default mapping.
     */
    static void init(Context setContext) {
        context = setContext;

        new SettingDetail(context.getString(R.string.orientation), context.getString(R.string.orientationDescription),
                0, R.string.vertical, R.string.horizontal).add();
        new SettingDetail(context.getString(R.string.scrollMode), context.getString(R.string.scrollModeDescription),
                0, R.string.forward, R.string.reverse).add();
        new SettingDetail(context.getString(R.string.touchWarning), context.getString(R.string.touchWarningDescription),
                0, R.string.enabled, R.string.disabled).add();
        new SettingDetail(context.getString(R.string.cursorMode), context.getString(R.string.cursorModeDescription),
                0, R.string.relative, R.string.absolute).add();

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

            // Save the default actionToTask file
            saveJsonFile();
        }
    }

    /**
     * Get the current mapping from action to task. All functional outer controls are excluded.
     *
     * @return current mapping
     */
    static SparseArray<TaskDetail> getCurrentMappingsDuplicated() {
        SparseArray<TaskDetail> toReturn = new SparseArray<>();
        for (int i = 0; i < actionToTask.size(); i++) {
            TaskDetail detail = actionToTask.valueAt(i);
            toReturn.put(actionToTask.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    static SparseArray<TaskDetail> getCurrentMappings() {
        return actionToTask;
    }

    static ArrayList<SettingDetail> getCurrentSettings() {
        return settingDetail;
    }

    static ArrayList<DeviceDetail> getCurrentDevices() {
        return deviceDetails;
    }

    static void updateAllSetting() {
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
     * Load the settings from local json file "settingDetails.json".
     * <p>
     * After loading, actionToTask and generalSettings will be modified according to the file.
     *
     * @throws FileNotFoundException If local file does not exist.
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

        int setting = 0;
        JsonArray generalSettings = (JsonArray) jsonObject.get("generalSettings");
        for (JsonElement o : generalSettings) {
            JsonObject individualSetting = (JsonObject) o;
            int status = individualSetting.get("status").getAsInt();
            settingDetail.get(setting++).setCurrentIdx(status);
        }

        deviceDetails.clear();
        JsonArray deviceList = (JsonArray) jsonObject.get("devices");
        for (JsonElement o : deviceList) {
            JsonObject individualDevice = (JsonObject) o;
            String deviceName = individualDevice.get("name").getAsString();
            String deviceMac = individualDevice.get("mac").getAsString();
            deviceDetails.add(new DeviceDetail(deviceMac, deviceName));
        }

        DeviceDetail.setIndexSelected(jsonObject.get("currentlySelected").getAsInt());
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
        for (SettingDetail setting : settingDetail) {
            JsonObject individualSetting = new JsonObject();
            int status = setting.getCurrentIdx();
            individualSetting.addProperty("status", status);
            generalSettings.add(individualSetting);
        }
        toSave.add("generalSettings", generalSettings);

        JsonArray deviceList = new JsonArray();
        for (DeviceDetail device : deviceDetails) {
            JsonObject individualDevice = new JsonObject();
            individualDevice.addProperty("name", device.getDeviceName());
            individualDevice.addProperty("mac", device.getRawMac());
            deviceList.add(individualDevice);
        }
        toSave.add("devices", deviceList);
        toSave.addProperty("currentlySelected", DeviceDetail.getIndexSelected());

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

    static String getSetting(int setting) {
        return settingDetail.get(setting).getCurrentState();
    }
}
