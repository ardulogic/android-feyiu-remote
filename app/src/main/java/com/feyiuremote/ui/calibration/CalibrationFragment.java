package com.feyiuremote.ui.calibration;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCalibrationBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationRunnable;
import com.feyiuremote.libs.Feiyu.calibration.ICalibrationListener;
import com.feyiuremote.libs.Feiyu.calibration.commands.FinaliseCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationLockedCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StartCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StopCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.WaitCommand;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class CalibrationFragment extends Fragment {

    private static final String TAG = "CalibrationFragment";
    private FragmentCalibrationBinding binding;
    private MainActivity mainActivity;
    private BluetoothViewModel bluetoothViewModel;
    private CalibrationDB calibrationDb;
    private CalibrationPresetDbHelper presetDb;
    private CalibrationRunnable calibrationRunnable;
    private CalibrationViewModel calibrationModel;

    private static final LinkedHashMap<Integer, int[]> CAL_MAP = new LinkedHashMap<Integer, int[]>() {{
        put(25, new int[]{65, 90, 100, 105, 135, 170, 200, 205, 235, 240});
        put(60, new int[]{55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225, 230, 235, 240});
        put(230, new int[]{52, 53, 54, 55, 56, 57, 58, 60, 61, 62, 64, 66, 68, 70, 71, 72, 74, 76, 78, 79, 80, 82, 83, 85, 90, 100, 110, 130, 140});
    }};

// For Debugging
//    private static final LinkedHashMap<Integer, int[]> CAL_MAP =  new LinkedHashMap<Integer, int[]>() {{
//        put(25, new int[]{65, 90});
//        put(60, new int[]{55, 60});
//        put(230, new int[]{52, 53});
//    }};


    private int joyValueIndex = 0;
    private int joySensitivityIndex = 0;
    private boolean isCalibrating = false;

    private int currentIteration = 0;
    private int stageIndex = 0;
    private final int maxStageIndex = 5;
    private int totalIterations;
    private long calibrationStartTime = 0L;

    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initViewModels();
        setupBinding(inflater, container);
        initializeDatabaseHelpers();

        totalIterations = calculateTotalIterations();

        setupCalibrationButtons();

        return binding.getRoot();
    }

    private void initViewModels() {
        calibrationModel = new ViewModelProvider(this).get(CalibrationViewModel.class);
        bluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
    }

    private void setupBinding(@NonNull LayoutInflater inflater, ViewGroup container) {
        binding = FragmentCalibrationBinding.inflate(inflater, container, false);

        calibrationModel.getPosText().observe(getViewLifecycleOwner(), binding.textGimbalStatus::setText);

        calibrationModel.getCalResultCv().observe(getViewLifecycleOwner(), (ContentValues cv) -> {
            printCalibrationTable(cv);
        });

        calibrationModel.mProgressJoySens.observe(getViewLifecycleOwner(), progress -> {
            ObjectAnimator animator = ObjectAnimator.ofInt(binding.progressBarJoySens, "progress", binding.progressBarJoySens.getProgress(), progress);
            animator.setDuration(500); // Duration in milliseconds
            animator.setInterpolator(new LinearInterpolator()); // Smooth animation
            animator.start();
        });

        calibrationModel.mTextJoySens.observe(getViewLifecycleOwner(), binding.textViewJoySens::setText);
        calibrationModel.mProgressJoyVal.observe(getViewLifecycleOwner(), progress -> {
            ObjectAnimator animator = ObjectAnimator.ofInt(binding.progressBarJoyVal, "progress", binding.progressBarJoyVal.getProgress(), progress);
            animator.setDuration(500); // Animation duration in milliseconds
            animator.setInterpolator(new LinearInterpolator()); // Optional: Smooth deceleration effect
            animator.start();
        });

        calibrationModel.mTextJoyVal.observe(getViewLifecycleOwner(), binding.textViewJoyVal::setText);
        calibrationModel.mTextProgress.observe(getViewLifecycleOwner(), binding.textCalibrationProgress::setText);
        calibrationModel.mPercProgress.observe(getViewLifecycleOwner(), binding.circularProgressBar::setProgress);

        bluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        mainActivity = (MainActivity) getActivity();
    }

    private void printCalibrationTable(ContentValues cv) {
        binding.tableCalibration.removeAllViews();

        for (String key : calibrationDb.getColumnNames()) {
            // Create a new TableRow
            TableRow tableRow = new TableRow(getContext());

            // Create a TextView for the key (first column)
            TextView keyTextView = new TextView(getContext());
            keyTextView.setText(key);
            keyTextView.setGravity(Gravity.START);
            keyTextView.setPadding(8, 8, 8, 8);

            // Create a TextView for the value (second column)
            TextView valueTextView = new TextView(getContext());
            valueTextView.setText(cv.getAsString(key));
            valueTextView.setGravity(Gravity.START);
            valueTextView.setPadding(8, 8, 8, 8);

            // Add the TextViews to the TableRow
            tableRow.addView(keyTextView);
            tableRow.addView(valueTextView);

            // Add the TableRow to the TableLayout
            binding.tableCalibration.addView(tableRow);
        }
    }

    private void initializeDatabaseHelpers() {
        calibrationDb = new CalibrationDB(mainActivity);
        presetDb = new CalibrationPresetDbHelper(mainActivity);
    }

    private int calculateTotalIterations() {
        return CAL_MAP.values().stream().mapToInt(values -> values.length).sum() * (maxStageIndex + 1);
    }

    private void setupCalibrationButtons() {
        binding.buttonCalibrate.setOnClickListener(view -> toggleCalibration());
        binding.buttonSave.setOnClickListener(view -> saveCalibrationAsPreset());
    }

    private void toggleCalibration() {
        if (isCalibrating) {
            stopCalibration();
        } else {
            startCalibration();
        }
    }

    private void stopCalibration() {
        isCalibrating = false;
    }

    private void startCalibration() {
        resetCalibration();
        calibrationRunnable = createCalibrationRunnable(0);
        calibrationRunnable.start();
        isCalibrating = true;

        updateProgress();
    }

    private void resetCalibration() {
        stageIndex = 0;
        joySensitivityIndex = 0;
        joyValueIndex = 0;
        currentIteration = 0;
        calibrationStartTime = System.currentTimeMillis();
    }

    private CalibrationRunnable createCalibrationRunnable(int stage) {
        Log.d(TAG, "Creating new calibration runnable");

        if (stage == 0) {
            return new CalibrationRunnable(
                    "locked",
                    getJoySensitivity(),
                    getJoyValue(),
                    buildCalibrationCommandsLockedAxis(mainActivity.mBluetoothLeService, getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        } else if (stage == 1) {
            return new CalibrationRunnable(
                    "locked",
                    getJoySensitivity(),
                    -getJoyValue(),
                    buildCalibrationCommandsLockedAxis(mainActivity.mBluetoothLeService, -getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        } else if (stage == 2) {
            return new CalibrationRunnable(
                    "pan_only",
                    getJoySensitivity(),
                    getJoyValue(),
                    buildCalibrationCommandsPanAxis(mainActivity.mBluetoothLeService, getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        } else if (stage == 3) {
            return new CalibrationRunnable(
                    "pan_only",
                    getJoySensitivity(),
                    -getJoyValue(),
                    buildCalibrationCommandsPanAxis(mainActivity.mBluetoothLeService, -getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        } else if (stage == 4) {
            return new CalibrationRunnable(
                    "tilt_only",
                    getJoySensitivity(),
                    getJoyValue(),
                    buildCalibrationCommandsTiltAxis(mainActivity.mBluetoothLeService, getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        } else if (stage == 5) {
            return new CalibrationRunnable(
                    "tilt_only",
                    getJoySensitivity(),
                    -getJoyValue(),
                    buildCalibrationCommandsTiltAxis(mainActivity.mBluetoothLeService, -getJoyValue()),
                    mainActivity.mBluetoothLeService,
                    calibrationListener
            );
        }

        return null;
    }

    private int getJoySensitivity() {
        Log.d(TAG, Arrays.toString(CAL_MAP.keySet().toArray()));
        return (int) CAL_MAP.keySet().toArray()[joySensitivityIndex];
    }

    private int getJoyValue() {
        return CAL_MAP.get(getJoySensitivity())[joyValueIndex];
    }

    private void saveCalibrationAsPreset() {
//        presetDb.deleteAll();
//        CAL_MAP.forEach((joySens, joyVals) -> {
//            for (int joyVal : joyVals) {
//                for (ContentValues row : calibrationDb.getByJoyState(joySens, joyVal, CalibrationDbHelper.LOCKED)) {
//                    presetDb.create(row);
//                }
//            }
//        });
//        calibrationModel.mCalResText.postValue("Calibration saved!");
    }

    private final ICalibrationListener calibrationListener = new ICalibrationListener() {
        @Override
        public void onCalFinished(ContentValues cv) {
            currentIteration++;
            updateProgress();

            calibrationModel.mCalResText.postValue(cv.toString());
            calibrationModel.mCalibrationCv.postValue(cv);
            calibrationDb.updateOrCreate(cv);

            if (isCalibrating) {
                proceedToNextCalibration();
            }
        }
    };

    private void updateProgress() {
        float progress = calculateProgress();
        calibrationModel.mPercProgress.setValue((int) progress);
        calibrationModel.mTextProgress.setValue(String.format("%d%%", (int) progress));

        String remainingTime = calculateRemainingTime(progress);
        calibrationModel.mTextProgress.setValue(remainingTime);

        updateProgressIndicators();
    }

    private float calculateProgress() {
        return ((float) currentIteration / totalIterations) * 100;
    }

    private String calculateRemainingTime(float progress) {
        long elapsedTime = System.currentTimeMillis() - calibrationStartTime;
        long estimatedTotalTime = (long) (elapsedTime / (progress / 100));
        long remainingTimeMs = estimatedTotalTime - elapsedTime;

        long minutes = remainingTimeMs / 60000;
        long seconds = (remainingTimeMs % 60000) / 1000;
        return (minutes >= 1) ? minutes + " min" : seconds + " s";
    }

    private void proceedToNextCalibration() {
        stageIndex++;

        if (stageIndex <= maxStageIndex) {
            calibrationRunnable = createCalibrationRunnable(stageIndex);
        } else {
            boolean notFinished = incrementCalibrationIndices();
            if (notFinished) {
                stageIndex = 0;
                calibrationRunnable = createCalibrationRunnable(stageIndex);
            }
        }

        if (calibrationRunnable != null) {
            Log.d(TAG, "Starting calibration runnable");
            calibrationRunnable.start();
        }
    }

    private boolean incrementCalibrationIndices() {
        if (joyValueIndex == CAL_MAP.get(getJoySensitivity()).length - 1) {
            joyValueIndex = 0;
            joySensitivityIndex++;
        } else {
            joyValueIndex++;
        }

        if (joySensitivityIndex == CAL_MAP.size()) {
            stopCalibration();
            resetProgressIndicators();
            return false;
        }

        return true;
    }

    private void resetProgressIndicators() {
        calibrationModel.mProgressJoySens.postValue(0);
        calibrationModel.mProgressJoyVal.postValue(0);
        calibrationModel.mTextJoySens.postValue("Joystick Sensitivity");
        calibrationModel.mTextJoyVal.postValue("Joystick Value");
    }

    private void updateProgressIndicators() {
        int stageCount = maxStageIndex + 1;

        double joySensProgress = ((double) (joySensitivityIndex + 1) / CAL_MAP.keySet().size() * 100);
        double joyValProgress = ((double) (joyValueIndex * (maxStageIndex + 1) + stageIndex + 1) / (CAL_MAP.get(getJoySensitivity()).length * stageCount) * 100);

        calibrationModel.mProgressJoySens.postValue((int) joySensProgress);
        calibrationModel.mProgressJoyVal.postValue((int) joyValProgress);

        calibrationModel.mTextJoySens.postValue("Joystick Sensitivity: " + getJoySensitivity());
        calibrationModel.mTextJoyVal.postValue("Joystick Value: " + getJoyValue());
    }

    final Observer<byte[]> btCharacteristicPositionObserver = new Observer<byte[]>() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onChanged(@Nullable final byte[] value) {
            FeyiuState.getInstance().update(value);

            String position = FeyiuState.getInstance().angle_tilt.posToString() + " | " +
                    FeyiuState.getInstance().angle_pan.posToString() + " | " +
                    FeyiuState.getInstance().angle_yaw.posToString();

            binding.textGimbalStatus.setText(Html.fromHtml(position, Html.FROM_HTML_MODE_LEGACY));

            if (calibrationRunnable != null) {
                calibrationRunnable.onGimbalUpdate();
            }
        }
    };


    public LinkedList<GimbalCommand> buildCalibrationCommandsLockedAxis(BluetoothLeService mBt, int mJoyVal) {
        LinkedList<GimbalCommand> commandQueue = new LinkedList<>();

        commandQueue.add(new StartCommand(mBt, mJoyVal));

        for (int i = 0; i < 20; i++) {
            commandQueue.add(new MoveCalibrationLockedCommand(mBt, mJoyVal));
        }

        commandQueue.add(new StopCommand(mBt));

        for (int i = 0; i < 5; i++) {
            commandQueue.add(new WaitCommand(mBt));
        }

        commandQueue.add(new FinaliseCommand(mBt));

        return commandQueue;
    }

    public LinkedList<GimbalCommand> buildCalibrationCommandsPanAxis(BluetoothLeService mBt, int mJoyVal) {
        LinkedList<GimbalCommand> commandQueue = new LinkedList<>();

        commandQueue.add(new StartCommand(mBt, mJoyVal));

        for (int i = 0; i < 20; i++) {
            commandQueue.add(new MoveCalibrationCommand(mBt, mJoyVal, 0));
        }

        commandQueue.add(new StopCommand(mBt));

        for (int i = 0; i < 5; i++) {
            commandQueue.add(new WaitCommand(mBt));
        }

        commandQueue.add(new FinaliseCommand(mBt));

        return commandQueue;
    }

    public LinkedList<GimbalCommand> buildCalibrationCommandsTiltAxis(BluetoothLeService mBt, int mJoyVal) {
        LinkedList<GimbalCommand> commandQueue = new LinkedList<>();

        commandQueue.add(new StartCommand(mBt, mJoyVal));

        for (int i = 0; i < 20; i++) {
            commandQueue.add(new MoveCalibrationCommand(mBt, 0, mJoyVal));
        }

        commandQueue.add(new StopCommand(mBt));

        for (int i = 0; i < 5; i++) {
            commandQueue.add(new WaitCommand(mBt));
        }

        commandQueue.add(new FinaliseCommand(mBt));

        return commandQueue;
    }

}
