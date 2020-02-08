package com.example.websocketTest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

class Controls {

    /**
     * Identification int for starting an activity of adding new mapping.
     */
    static final int ACTIVITY_ADD_MAPPING = 0;

    /**
     * Length of mac address.
     */
    static final int MAC_LENGTH = 12;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static Vibrator vibrator;
    private static AudioManager audioManager;
    private static ClipboardManager clipboard;


    /**
     * Configuration of tasks. Task what defines to be sent to pc.
     */
    static class TaskDetail {

        /**
         * Mapping action (inner control) to task (outer control).
         */
        private static SparseArray<TaskDetail> actionToTask = new SparseArray<>();


        /**
         * Mapping task to its own TaskDetail.
         */
        private static SparseArray<TaskDetail> taskDetails = new SparseArray<>();

        /**
         * Get the readable description corresponds to the task.
         *
         * @param taskTypeRepresentation the string representing the task
         * @return string which describes the function of the task
         */
        static String getReadableTask(int taskTypeRepresentation) {
            return taskDetails.get(taskTypeRepresentation).getDescription();
        }

        /**
         * Get the corresponding outer control task of a outer control string.
         *
         * @param taskTypeRepresentation string representation of a outer control string
         * @return TaskDetail instance corresponds to tht specific outer control string
         */
        static TaskDetail correspondsTo(int taskTypeRepresentation) {
            return taskDetails.get(taskTypeRepresentation);
        }

        /**
         * string representation of task.
         */
        private int task;

        /**
         * Readable description of the task, using @string resources..
         */
        private Integer description;

        /**
         * Whether the control is a basic control. Basic controls are not allowed to be modified in
         * the settings.
         */
        private boolean basicControl;

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
         * @param isBasicControl is task a basic control
         * @param canRepeat      can the task be repeated
         */
        TaskDetail(int setTask, Integer setDescription, boolean isBasicControl, boolean canRepeat) {
            task = setTask;
            description = setDescription;
            basicControl = isBasicControl;
            canBeRepeated = canRepeat;
        }

        /**
         * Get a duplication of the detail.
         *
         * @return a duplication of the detail
         */
        TaskDetail duplicate() {
            return new TaskDetail(task, description, basicControl, canBeRepeated);
        }

        /**
         * Get the representation string of the task.
         *
         * @return representation string of the task
         */
        int getTask() {
            return task;
        }

        /**
         * Get the description of the task.
         *
         * @return description of the task
         */
        String getDescription() {
            if (description == null) {
                return null;
            }
            if (basicControl) {
                return String.format("%s %s", context.getString(description), context.getString(R.string.basicControl));
            }
            return context.getString(description);
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
            taskDetails.put(task, this);
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
     * Configuration of each setting.
     */
    static class SettingDetail {

        /**
         * Setting tab number user will see when opening up settings.
         */
        private static int currentSettingTab = 0;

        /**
         * Get page index of the setting the user is now on.
         *
         * @return current index of the setting page the user is on
         */
        static int getCurrentSettingTab() {
            return currentSettingTab;
        }

        /**
         * Set page index of the setting the user is now on.
         *
         * @param currentIdx index of the setting page
         */
        static void setCurrentSettingTab(int currentIdx) {
            currentSettingTab = currentIdx;
        }

        /**
         * Get the current status of a setting.
         *
         * @param setting index of the setting trying to find
         * @return current status of the setting
         */
        static String getStatusOfSetting(int setting) {
            return SettingDetail.settingDetails.get(setting).getCurrentState();
        }

        /**
         * ArrayList storing all the settings.
         */
        private static ArrayList<SettingDetail> settingDetails = new ArrayList<>();

        /**
         * Description of the setting. Using @string resources.
         */
        private int settingDescription;

        /**
         * Detailed explanation of the setting, including the function and usage of every choice in
         * allStates array.
         */
        private int detailedDescription;

        /**
         * The current choice of the state.
         */
        private int currentIdx;

        /**
         * All the choice available of a setting.
         */
        private int[] allStates;

        /**
         * Constructing a new setting using the description of the setting, detailed description of
         * the setting, the current selected state and all the available choices.
         *
         * @param setSettingDescription  description of the setting, will appear on the setting button
         * @param setDetailedDescription detailed description of the setting, including explanation
         *                               of each choice
         * @param setCurrentIdx          index of the state user currently selected
         * @param setAllStates           all the choices of the setting
         */
        SettingDetail(int setSettingDescription, int setDetailedDescription, int setCurrentIdx,
                      int... setAllStates) {
            if (setCurrentIdx > setAllStates.length) {
                throw new IllegalArgumentException(String.format(
                        "Current index cannot exceed number of states. Try to access %d, but only have %d states.",
                        setCurrentIdx, setAllStates.length));
            }
            settingDescription = setSettingDescription;
            detailedDescription = setDetailedDescription;
            currentIdx = setCurrentIdx;
            allStates = setAllStates;
        }

        /**
         * Get detailed description of a setting.
         *
         * @return detailed description of a setting
         */
        String getDetailedDescription() {
            return context.getString(detailedDescription);
        }

        /**
         * Get current state fo the setting.
         *
         * @return current state fo the setting
         */
        String getCurrentState() {
            return context.getString(allStates[currentIdx]);
        }

        /**
         * Get the combined description of the setting and its current state.
         *
         * @return combined description used when displaying on button
         */
        String getSettingDescriptionAndState() {
            return String.format(Locale.getDefault(), "%s : %s", context.getString(settingDescription), context.getString(allStates[currentIdx]));
        }

        /**
         * Get the index of current state in all states.
         *
         * @return index of current state
         */
        int getCurrentIdx() {
            return currentIdx;
        }

        /**
         * Get all the possible status of the setting.
         *
         * @return an array of strings representing all states that can be chosen
         */
        String[] getAllStatus() {
            String[] toReturn = new String[allStates.length];
            for (int i = 0; i < toReturn.length; i++) {
                toReturn[i] = context.getString(allStates[i]);
            }
            return toReturn;
        }

        /**
         * Set current state index. (Or switch state)
         *
         * @param idx index of current state.
         */
        void changeSetting(int idx) {
            currentIdx = idx;
        }

        /**
         * Adding the detail to list for it to be functional, used when initialization
         */
        void add(int index) {
            settingDetails.add(index, this);
        }

    }

    /**
     * Configuration of devices.
     */
    static class DeviceDetail {

        /**
         * ArrayList storing all the devices entered.
         */
        private static ArrayList<DeviceDetail> deviceDetails = new ArrayList<>();

        /**
         * Index of the current device that is selected.
         */
        private static int indexSelected = -1;

        /**
         * Get the index of the currently selected device.
         *
         * @return the index of the currently selected device
         */
        static int getIndexSelected() {
            return indexSelected;
        }

        /**
         * Check of the input mac is valid. For it to be valid, the string will be 12 characters long,
         * containing only numbers and both upper and lower case of the letter 'a', 'b', 'c', 'd', 'e',
         * and 'f'.
         *
         * @param mac target device bluetooth mac
         * @return whether the mac address is valid
         */
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

        /**
         * Check whether the mac is duplicated with current existing mac addresses.
         */
        static boolean isDuplicated(String mac) {
            for (DeviceDetail detail : deviceDetails) {
                if (detail.macAddress.equals(mac)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Set the current selected device to the device at specific index. Also update the mac to
         * PermanentConnection.
         *
         * @param index index of currently selected device
         */
        static void setIndexSelected(int index) {
            if (index != -1) {
                if (index >= deviceDetails.size()) {
                    throw new IllegalArgumentException(String.format(
                            "Index of device exceeds device count. Currently requesting %d, but only have%d",
                            index, deviceDetails.size()));
                }
                PermanentConnection.setServerMac(deviceDetails.get(index).getMacAddress());
            } else {
                PermanentConnection.setServerMac(null);
            }
            indexSelected = index;
        }

        /**
         * Mac address of the device.
         */
        private String macAddress;

        /**
         * User defined name for the device.
         */
        private String deviceName;

        /**
         * Constructing a new device using the mac address of the device, and the user defined name
         * of the device. Mac address stored will be upper cased.
         *
         * @param setMacAddress mac address of the device
         * @param setDeviceName user defined name of the device
         */
        DeviceDetail(String setMacAddress, String setDeviceName) {
            if (!isValidMac(setMacAddress) || isDuplicated(setMacAddress)) {
                throw new IllegalArgumentException("Not a valid mac");
            }
            macAddress = setMacAddress.toUpperCase(Locale.getDefault());
            deviceName = setDeviceName;
        }

        /**
         * Get the formatted string of the device mac. Formatted mac address look like "XX:XX:XX:XX:XX:XX"
         *
         * @return formatted string of device mac
         */
        String getMacAddress() {
            return String.format("%s:%s:%s:%s:%s:%s", macAddress.substring(0, 2), macAddress.substring(2, 4),
                    macAddress.substring(4, 6), macAddress.substring(6, 8), macAddress.substring(8, 10), macAddress.substring(10, 12));
        }

        /**
         * Set name of the device.
         *
         * @param newDeviceName the user defined name for the device
         */
        void setDeviceName(String newDeviceName) {
            deviceName = newDeviceName;
        }

        /**
         * Get name of the device.
         *
         * @return user defined name for the device
         */
        String getDeviceName() {
            return deviceName;
        }

    }

    static class SensitivitySetting {
        private static List<SensitivitySetting> sensitivitySettings = new ArrayList<>();

        static final int MIN = 20;
        static final int MAX = 100;
        private int multiplicativeFactor;
        private int sensitivity;

        SensitivitySetting(int setFactor, int setSensitivity) {
            multiplicativeFactor = setFactor;
            sensitivity = setSensitivity;
        }

        int getSensitivity() {
            return sensitivity;
        }

        float getRealSensitivity() {
            return (float) (MAX + MIN - sensitivity) / multiplicativeFactor;
        }

        void setSensitivity(int newSensitivity) {
            sensitivity = newSensitivity;
        }

        void add() {
            sensitivitySettings.add(this);
        }
    }

    /**
     * Size of the phone screen, used when cursor move state is absolute.
     */
    static CoordinatePair phoneScreenSize;

    /* All inner controls
    Inner controls responsible for transmitting data inside the application, to send the data, a
    translation os inner control to outer control is required. */
    // Action
    static final byte TAP = 0b000;                  //0
    static final byte MOVE = 0b001;                 //1
    static final byte LONG_TAP = 0b010;             //2
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
    static final byte INPUT_TEXT = 0b110000;        //48 Functional Control
    static final byte MOVE_CANCEL = 0b110001;       //49 Functional Control
    static final byte HEARTBEAT_ACTION = 0b110010;  //50 Functional Control
    static final byte SUSPEND_ACTION = 0b110011;    //51 Functional Control
    /* Combined Action is the combination of one Action and one Action FingerCount.
    It represents the action that user did on the screen. */

    private static final int CLICK = 1;
    private static final int RIGHT_CLICK = 2;
    private static final int DOUBLE_CLICK = 3;
    private static final int MOVE_CURSOR_RELATIVE = 4;
    private static final int MOVE_CURSOR_ABSOLUTE = 5;
    private static final int SELECT = 6;
    private static final int SCROLL = 7;
    private static final int UNDO = 8;
    private static final int COPY = 9;
    private static final int PASTE = 10;
    private static final int CUT = 11;
    private static final int RETURN_TO_DESKTOP = 12;
    private static final int ENABLE_TASK_MODE = 13;
    private static final int SWITCH_APPLICATION = 14;
    private static final int SWITCH_TAB = 15;
    private static final int INPUT_TEXT_FUNCTIONAL = 16;

    private static final int ACTION_NOT_FOUND_FUNCTIONAL = 100;
    private static final int CANCEL_LAST_ACTION_FUNCTIONAL = 101;
    private static final int HEARTBEAT_FUNCTIONAL = 102;
    private static final int EXITING_TOUCH_PAD_FUNCTIONAL = 103;
    private static final int SUSPEND_CONNECTION_FUNCTIONAL = 104;

    // Functional outer controls
    private static final TaskDetail PASTE_TEXT = new TaskDetail(INPUT_TEXT_FUNCTIONAL, null, true, true);
    private static final TaskDetail CANCEL_LAST_ACTION = new TaskDetail(CANCEL_LAST_ACTION_FUNCTIONAL, null, true, true);
    private static final TaskDetail HEARTBEAT = new TaskDetail(HEARTBEAT_FUNCTIONAL, null, true, true);
    private static final TaskDetail SUSPEND = new TaskDetail(SUSPEND_CONNECTION_FUNCTIONAL, null, true, false);
    static final TaskDetail ACTION_NOT_FOUND = new TaskDetail(ACTION_NOT_FOUND_FUNCTIONAL, null, true, false);

    // Coordinate pairs used in comparisons
    static final CoordinatePair NOT_STARTED = new CoordinatePair(-1, -1);
    static final CoordinatePair ZERO = new CoordinatePair(0, 0);

    static final int ORIENTATION_SETTING = 0;
    static final int SCROLL_MODE_SETTING = 1;
    static final int TOUCH_WARNING_SETTING = 2;
    static final int CURSOR_MODE_SETTING = 3;
    private static final int VIBRATION_SETTING = 4;


    /**
     * Initialize all the outer controls. If there is a file stored in the device which describes all
     * the settings, read the file for all setting; otherwise, use default setting..
     */
    @SuppressWarnings("unused")
    static void init(Context setContext) {
        context = setContext;

        SettingDetail.settingDetails.clear();
        new SettingDetail(R.string.orientation, R.string.orientationDescription,
                0, R.string.vertical, R.string.horizontal).add(ORIENTATION_SETTING);
        new SettingDetail(R.string.scrollMode, R.string.scrollModeDescription,
                1, R.string.forward, R.string.reverse).add(SCROLL_MODE_SETTING);
        new SettingDetail(R.string.touchWarning, R.string.touchWarningDescription,
                0, R.string.enabled, R.string.disabled).add(TOUCH_WARNING_SETTING);
        new SettingDetail(R.string.cursorMode, R.string.cursorModeDescription,
                0, R.string.relative, R.string.absolute).add(CURSOR_MODE_SETTING);
        new SettingDetail(R.string.vibrationMode, R.string.vibrationModeDescription,
                2, R.string.enabled, R.string.disabled, R.string.followSystem).add(VIBRATION_SETTING);

        SensitivitySetting.sensitivitySettings.clear();
        int averageSensitivity = (SensitivitySetting.MAX + SensitivitySetting.MIN) / 2;
        new SensitivitySetting(80, averageSensitivity).add();
        new SensitivitySetting(6, averageSensitivity).add();
        new SensitivitySetting(2, averageSensitivity).add();
        new SensitivitySetting(2, averageSensitivity).add();
        new SensitivitySetting(2, averageSensitivity).add();

        /* All outer controls
        Outer controls are responsible for transmission and adding new mapping. Each outer control
        corresponds to a combination of inner controls. */
        TaskDetail click = new TaskDetail(CLICK, R.string.click, true, false).add();
        TaskDetail rightClick = new TaskDetail(RIGHT_CLICK, R.string.rightClick, true, false).add();
        TaskDetail doubleClick = new TaskDetail(DOUBLE_CLICK, R.string.doubleClick, true, false).add();
        TaskDetail moveCursorRelative = new TaskDetail(MOVE_CURSOR_RELATIVE, R.string.moveCursor, true, true).add();
        TaskDetail moveCursorAbsolute = new TaskDetail(MOVE_CURSOR_ABSOLUTE, R.string.moveCursor, true, true).add();
        TaskDetail select = new TaskDetail(SELECT, R.string.taskSelect, true, false).add();
        TaskDetail scroll = new TaskDetail(SCROLL, R.string.scroll, true, true).add();
        TaskDetail undo = new TaskDetail(UNDO, R.string.undo, false, false).add();
        TaskDetail copy = new TaskDetail(COPY, R.string.copy, false, false).add();
        TaskDetail paste = new TaskDetail(PASTE, R.string.paste, false, false).add();
        TaskDetail cut = new TaskDetail(CUT, R.string.cut, false, false).add();
        TaskDetail returnToDesktop = new TaskDetail(RETURN_TO_DESKTOP, R.string.returnToDesktop, false, false).add();
        TaskDetail enableTaskMode = new TaskDetail(ENABLE_TASK_MODE, R.string.enableTaskMode, false, false).add();
        TaskDetail switchApplication = new TaskDetail(SWITCH_APPLICATION, R.string.switchApplication, false, true).add();
        TaskDetail switchTab = new TaskDetail(SWITCH_TAB, R.string.switchTab, false, true).add();
        /* This list can be extended
        If the description contains R.string.BasicControl, it will not be allowed to be modified in the generalSettings
        Format of any extension format is as follows:
            TaskDetail NAME = new TaskDetail(stringRepresentation, intStringDescription, isBasicControl, canBeRepeated).add();     */


        // This will not be reached by the identifyAndSend method
        TaskDetail actionExitingTouchPad = new TaskDetail(EXITING_TOUCH_PAD_FUNCTIONAL, R.string.exitTouchPad, true, false).add();
        // Functional outer controls
        addMapping(ACTION_NOT_FOUND, PASTE_TEXT, CANCEL_LAST_ACTION, HEARTBEAT, SUSPEND);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        try {
            // Try to load the json file
            loadJsonFile();
        } catch (Exception e) {
            // If the file does not exist
            // Default mapping
            SparseArray<TaskDetail> actionToTask = getCurrentMappings();

            actionToTask.append(SINGLE_FINGER + TAP, click);
            actionToTask.append(SINGLE_FINGER + DOUBLE_TAP, doubleClick);
            actionToTask.append(TWO_FINGERS + TAP, rightClick);
            actionToTask.append(SINGLE_FINGER + MOVE, moveCursorRelative);
            actionToTask.append(SINGLE_FINGER + LONG_TAP, select);
            actionToTask.append(TWO_FINGERS + MOVE, scroll);
            actionToTask.append(THREE_FINGERS + MOVE_DOWN, returnToDesktop);
            actionToTask.append(THREE_FINGERS + MOVE_UP, enableTaskMode);
            actionToTask.append(THREE_FINGERS + MOVE_LEFT, switchApplication);
            actionToTask.append(THREE_FINGERS + MOVE_RIGHT, switchApplication);
            actionToTask.append(FOUR_FINGERS + MOVE_DOWN, actionExitingTouchPad);

            actionToTask.append(INPUT_TEXT, PASTE_TEXT);
            actionToTask.append(MOVE_CANCEL, CANCEL_LAST_ACTION);
            actionToTask.append(HEARTBEAT_ACTION, HEARTBEAT);
            actionToTask.append(SUSPEND_ACTION, SUSPEND);

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
        for (int i = 0; i < TaskDetail.actionToTask.size(); i++) {
            TaskDetail detail = TaskDetail.actionToTask.valueAt(i);
            toReturn.put(TaskDetail.actionToTask.keyAt(i), detail.duplicate());
        }
        return toReturn;
    }

    static SparseArray<TaskDetail> getCurrentMappings() {
        return TaskDetail.actionToTask;
    }

    static List<SettingDetail> getCurrentSettings() {
        return SettingDetail.settingDetails;
    }

    static List<DeviceDetail> getCurrentDevices() {
        return DeviceDetail.deviceDetails;
    }

    static SparseArray<TaskDetail> getAllTasks() {
        return TaskDetail.taskDetails;
    }

    static List<SensitivitySetting> getCurrentSensitivities() {
        return SensitivitySetting.sensitivitySettings;
    }

    static void updateAllSetting(boolean saveFile) {
        PermanentConnection.TouchEventMappingControl.updateMapping();
        changeCursorMoveMode(SettingDetail.settingDetails.get(CURSOR_MODE_SETTING).getCurrentState()
                .equals(context.getString(R.string.relative)));
        if (saveFile) {
            saveJsonFile();
        }
    }

    /**
     * Change current cursor move mode to specified mode.
     *
     * @param isRelative boolean value states whether cursor move state is relative or not
     */
    private static void changeCursorMoveMode(boolean isRelative) {
        if (isRelative) {
            TaskDetail.actionToTask.put(SINGLE_FINGER + MOVE, TaskDetail.taskDetails.get(MOVE_CURSOR_RELATIVE));
        } else {
            TaskDetail.actionToTask.put(SINGLE_FINGER + MOVE, TaskDetail.taskDetails.get(MOVE_CURSOR_ABSOLUTE));
        }
        PermanentConnection.TouchEventMappingControl.updateMapping();
    }

    /**
     * Load the settings from local json file "settingDetails.json".
     * <p>
     * After loading, actionToTask and generalSettings will be modified according to the file.
     *
     * @throws FileNotFoundException If local file does not exist.
     */
    private static void loadJsonFile() throws FileNotFoundException {
        Log.println(Log.INFO, "FileAccessing", "Loading settings file.");
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader inputStreamReader = new InputStreamReader(context.openFileInput("settingDetails.json"), StandardCharsets.UTF_8);
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            Log.println(Log.ERROR, "FileAccessing", "Settings file reading error.");
            throw new FileNotFoundException();
        }
        if (stringBuilder.toString().equals("")) {
            Log.println(Log.INFO, "FileAccessing", "Settings file does not exist.");
            throw new FileNotFoundException();
        }

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(stringBuilder.toString(), JsonObject.class);
        if (jsonObject == null) {
            throw new FileNotFoundException();
        }
        JsonArray mappingControls = (JsonArray) jsonObject.get("mappingControls");

        TaskDetail.actionToTask.clear();
        for (JsonElement o : mappingControls) {
            JsonObject individualMapping = (JsonObject) o;
            byte combinedAction = individualMapping.get("combinedAction").getAsByte();
            int outerControl = individualMapping.get("task").getAsInt();
            TaskDetail.actionToTask.put(combinedAction, TaskDetail.taskDetails.get(outerControl));
        }

        int setting = 0;
        JsonArray generalSettings = (JsonArray) jsonObject.get("generalSettings");
        for (JsonElement o : generalSettings) {
            JsonObject individualSetting = (JsonObject) o;
            int status = individualSetting.get("status").getAsInt();
            SettingDetail.settingDetails.get(setting++).changeSetting(status);
        }

        DeviceDetail.deviceDetails.clear();
        JsonArray deviceList = (JsonArray) jsonObject.get("devices");
        for (JsonElement o : deviceList) {
            JsonObject individualDevice = (JsonObject) o;
            String deviceName = individualDevice.get("name").getAsString();
            String deviceMac = individualDevice.get("mac").getAsString();
            DeviceDetail.deviceDetails.add(new DeviceDetail(deviceMac, deviceName));
        }

        SensitivitySetting.sensitivitySettings.clear();
        JsonArray sensitivityList = (JsonArray) jsonObject.get("sensitivities");
        for (JsonElement o : sensitivityList) {
            JsonObject individualSensitivity = (JsonObject) o;
            int multiplicativeFactor = individualSensitivity.get("factor").getAsInt();
            int sensitivity = individualSensitivity.get("sensitivity").getAsInt();
            SensitivitySetting.sensitivitySettings.add(new SensitivitySetting(multiplicativeFactor, sensitivity));
        }

        int currentDevice = jsonObject.get("currentlySelected").getAsInt();
        DeviceDetail.setIndexSelected(currentDevice);

        updateAllSetting(false);
    }

    /**
     * Save the settings to local json file "settingDetails.json".
     * <p>
     * Mapping info comes from actionToTask and general setting info comes from generalSettings.
     */
    private static void saveJsonFile() {
        Log.println(Log.INFO, "FileAccessing", "Writing settings file.");
        JsonObject toSave = new JsonObject();
        JsonArray mappingControls = new JsonArray();
        for (int i = 0; i < TaskDetail.actionToTask.size(); i++) {
            JsonObject individualMapping = new JsonObject();
            TaskDetail detail = TaskDetail.actionToTask.valueAt(i);
            byte combinedAction = (byte) TaskDetail.actionToTask.keyAt(i);
            assert detail != null;
            int outerControl = detail.getTask();
            individualMapping.addProperty("combinedAction", combinedAction);
            individualMapping.addProperty("task", outerControl);
            mappingControls.add(individualMapping);
        }
        toSave.add("mappingControls", mappingControls);

        JsonArray generalSettings = new JsonArray();
        for (SettingDetail setting : SettingDetail.settingDetails) {
            JsonObject individualSetting = new JsonObject();
            int status = setting.getCurrentIdx();
            individualSetting.addProperty("status", status);
            generalSettings.add(individualSetting);
        }
        toSave.add("generalSettings", generalSettings);

        JsonArray deviceList = new JsonArray();
        for (DeviceDetail device : DeviceDetail.deviceDetails) {
            JsonObject individualDevice = new JsonObject();
            individualDevice.addProperty("name", device.deviceName);
            individualDevice.addProperty("mac", device.macAddress);
            deviceList.add(individualDevice);
        }
        toSave.add("devices", deviceList);

        JsonArray sensitivityList = new JsonArray();
        for (SensitivitySetting sensitivity : SensitivitySetting.sensitivitySettings) {
            JsonObject individualSensitivity = new JsonObject();
            individualSensitivity.addProperty("factor", sensitivity.multiplicativeFactor);
            individualSensitivity.addProperty("sensitivity", sensitivity.sensitivity);
            sensitivityList.add(individualSensitivity);
        }
        toSave.add("sensitivities", sensitivityList);

        toSave.addProperty("currentlySelected", DeviceDetail.getIndexSelected());

        try {
            FileOutputStream outputStreamWriter = context.openFileOutput("settingDetails.json", Context.MODE_PRIVATE);
            outputStreamWriter.write(toSave.toString().getBytes());
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.println(Log.ERROR, "FileAccessing", "Settings file writing error.");
        }
    }

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
     * Make the phone vibrate if it has a vibrator and either it is on normal or vibrate state.
     */
    static void vibrate() {
        String currentState = SettingDetail.settingDetails.get(VIBRATION_SETTING).getCurrentState();
        int ringerMode = audioManager.getRingerMode();
        if (currentState.equals(context.getString(R.string.enabled))
                || (currentState.equals(context.getString(R.string.followSystem))
                && (ringerMode == AudioManager.RINGER_MODE_NORMAL || ringerMode == AudioManager.RINGER_MODE_VIBRATE))) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    static String getClipboardContent() {
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0 && clipData.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            String content = clipData.getItemAt(0).getText().toString();
            if (!(content.equals("") || content.equals(" "))) {
                return content;
            }
        }
        return null;
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
     * Change the usability of all the views under the root view group.
     *
     * @param enable usability of views
     * @param vg     root view group
     */
    static void setUsability(boolean enable, ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                setUsability(enable, (ViewGroup) child);
            }
        }
    }

    static byte convertNumFingersToInnerControl(int fingerNum) {
        return (byte) (8 * fingerNum);
    }

    /**
     * Get description by combined action and a taskDetails instance.
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
        if (numFingers > 5) {
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
            case LONG_TAP:
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
            case DOUBLE_TAP:
                toReturn += context.getString(R.string.doubleTap);
                break;
        }
        if (numFingers == 0) {
            return toReturn;
        }
        return toReturn + " --- " + detail.getDescription();
    }


    static class WindowManagement {
        static void enableImmersiveMode(Activity activity) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        static void showDialogSilence(AlertDialog dialog, Activity activity) {
            Objects.requireNonNull(dialog.getWindow()).setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    activity.getWindow().getDecorView().getSystemUiVisibility());
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(activity.getWindow().getDecorView(), activity.getWindow().getAttributes());
        }
    }
}
