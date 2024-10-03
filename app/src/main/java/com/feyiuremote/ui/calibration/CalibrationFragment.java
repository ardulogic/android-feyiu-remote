package com.feyiuremote.ui.calibration;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCalibrationBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationRunnable;
import com.feyiuremote.libs.Feiyu.calibration.ICalibrationListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class CalibrationFragment extends Fragment {

    private FragmentCalibrationBinding binding;
    private MainActivity mainActivity;
    private BluetoothViewModel mBluetoothViewModel;
    private CalibrationDbHelper mCalDb;
    private CalibrationPresetDbHelper mCalPresetDb;
    private CalibrationRunnable mCalRunnable;
    private CalibrationViewModel mCalibrationModel;

    private static HashMap<Integer, int[]> cal_map = new HashMap<Integer, int[]>() {{
        put(25, new int[]{65, 90, 100, 105, 135, 170, 200, 205, 235, 240});
        put(60, new int[]{55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225, 230, 235, 240});
        put(230, new int[]{52, 53, 54, 55, 56, 57, 58, 60, 61, 62, 64, 66, 68, 70, 71, 72, 74, 76, 78, 79, 80, 82, 83, 85, 90, 100, 110, 130, 140});
    }};

    private int i_joy_val = 0;
    private int i_joy_sen = 0;
    private int i_dir = -1;
    private boolean mCalibrating = false;

    private int curr_calib_iteration = 0;
    private int total_calib_iterations;

    private Long time_calib_start = 0L;
    private String curr_calib_name;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mCalibrationModel = new ViewModelProvider(this).get(CalibrationViewModel.class);
        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);

        binding = FragmentCalibrationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        mCalibrationModel.getPosText().observe(getViewLifecycleOwner(), binding.textGimbalStatus::setText);
        mCalibrationModel.getCalResultText().observe(getViewLifecycleOwner(), binding.textCalibrationResult::setText);

        mCalibrationModel.mProgressJoySens.observe(getViewLifecycleOwner(), binding.progressBarJoySens::setProgress);
        mCalibrationModel.mTextJoySens.observe(getViewLifecycleOwner(), binding.textViewJoySens::setText);
        mCalibrationModel.mProgressJoyVal.observe(getViewLifecycleOwner(), binding.progressBarJoyVal::setProgress);
        mCalibrationModel.mTextJoyVal.observe(getViewLifecycleOwner(), binding.textViewJoyVal::setText);
        mCalibrationModel.mTextProgress.observe(getViewLifecycleOwner(), binding.textCalibrationProgress::setText);
        mCalibrationModel.mPercProgress.observe(getViewLifecycleOwner(), binding.circularProgressBar::setProgress);

        mainActivity = (MainActivity) getActivity();

        mCalDb = new CalibrationDbHelper(mainActivity);
        mCalPresetDb = new CalibrationPresetDbHelper(mainActivity);
        total_calib_iterations = countCalibrationIterations();

        mBluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        binding.buttonCalibrate.setOnClickListener(view -> {
            if (!mCalibrating) {
                startCalibration();
            } else {
                stopCalibration();
            }
        });

        binding.buttonSave.setOnClickListener(view -> saveCalibrationAsPreset());

        return root;
    }

    private void stopCalibration() {
        mCalibrating = false;
    }

    private void startCalibration() {
        showInputDialog(getContext(), name -> {
            i_dir = -1; // Necessary to correct dir inversion in first attempt
            i_joy_sen = 0;
            i_joy_val = 0;
            curr_calib_iteration = 0;
            time_calib_start = System.currentTimeMillis();
            curr_calib_name = name;

            mCalRunnable = new CalibrationRunnable(
                    curr_calib_name,
                    getCalJoySens(),
                    getCalJoyVal(),
                    mainActivity.mBluetoothLeService,
                    mCalibrationListener
            );

            mCalRunnable.start();
            mCalibrating = true;
        });
    }

    private int getCalJoySens() {
        Object[] keys = cal_map.keySet().toArray();
        return (int) keys[i_joy_sen];
    }

    private int getCalJoyVal() {
        return cal_map.get(getCalJoySens())[i_joy_val];
    }

    private int countCalibrationIterations() {
        int count = 0;
        for (int key : cal_map.keySet()) {
            int[] values = cal_map.get(key);
            count += values.length;
        }

        return count * 2; // Pirmyn / atgal

    }

    public interface NameInputListener {
        void onNameEntered(String name);
    }


    public void showInputDialog(Context context, final NameInputListener listener) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // Get the layout inflater
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.calibration_dialog, null);

        // Set the view in the dialog
        alertDialogBuilder.setView(view);

        final EditText input = view.findViewById(R.id.editTextName);

        // Set dialog buttons and their actions
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String enteredName = input.getText().toString();
                        listener.onNameEntered(enteredName);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        // Create and show the dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void updateProgressBar() {
        // Interesting thing: curr_calib_iteration / total_calib_iterations * 100; wont work,
        // because first operation returns 0 as integer
        float progress = (float) curr_calib_iteration * 100 / total_calib_iterations;
        mCalibrationModel.mPercProgress.setValue((int) progress);
        mCalibrationModel.mTextProgress.setValue(String.valueOf((int) progress + "%"));

        long time_elapsed = System.currentTimeMillis() - time_calib_start;
        long estimated_total_time = (long) (time_elapsed / (progress / 100));
        long time_remain_ms = (estimated_total_time - time_elapsed);
        long time_remain_mins = time_remain_ms / 60000;
        long time_remain_secs = time_remain_ms / 1000;

        if (time_remain_mins >= 1) {
            mCalibrationModel.mTextProgress.setValue(time_remain_mins + " min");
        } else {
            mCalibrationModel.mTextProgress.setValue(time_remain_secs + " s");
        }
    }

    private void runNextCal() {
        if (i_dir == 0) {
            if (i_joy_val == cal_map.get(getCalJoySens()).length - 1) {
                i_joy_val = 0;
                i_joy_sen++;
            } else {
                i_joy_val++;
            }

            if (i_joy_sen == cal_map.size()) {
                mCalibrating = false;
                mCalibrationModel.mProgressJoySens.postValue(0);
                mCalibrationModel.mProgressJoyVal.postValue(0);
                mCalibrationModel.mTextJoySens.postValue("Joystick Sensistivity");
                mCalibrationModel.mTextJoyVal.postValue("Joystick Value");
                return;
            }

            mCalibrationModel.mProgressJoySens.postValue(i_joy_sen * 100 / (cal_map.size() - 1));
            mCalibrationModel.mProgressJoyVal.postValue(i_joy_val * 100 / (cal_map.get(getCalJoySens()).length - 1));
            mCalibrationModel.mTextJoySens.postValue("Joystick Sensistivity: " + getCalJoySens());
            mCalibrationModel.mTextJoyVal.postValue("Joystick Value: " + getCalJoyVal());

            mCalRunnable = new CalibrationRunnable(
                    curr_calib_name, getCalJoySens(),
                    getCalJoyVal(),
                    mainActivity.mBluetoothLeService,
                    mCalibrationListener
            );

            i_dir = 1;
        } else {
            mCalRunnable = new CalibrationRunnable(
                    curr_calib_name, getCalJoySens(),
                    -getCalJoyVal(),
                    mainActivity.mBluetoothLeService,
                    mCalibrationListener
            );

            i_dir = 0;
        }

        mCalRunnable.start();
    }

    private void saveCalibrationAsPreset() {
        mCalPresetDb.deleteAll();

        for (Map.Entry<Integer, int[]> entry : cal_map.entrySet()) {
            int joy_sen = entry.getKey();
            int[] joy_vals = entry.getValue();
            for (int joy_val : joy_vals) {
                ArrayList<ContentValues> rows = mCalDb.getByJoyState(joy_sen, joy_val);

                for (ContentValues row : rows) {
                    mCalPresetDb.create(row);
                }
            }
        }

        mCalibrationModel.mCalResText.postValue("Calibration saved!");
    }

    private ICalibrationListener mCalibrationListener = new ICalibrationListener() {
        @Override
        public void onCalFinished(ContentValues cv) {
            curr_calib_iteration++;
            updateProgressBar();

            StringBuilder cvString = new StringBuilder("Calibrated:\n");

            for (Map.Entry<String, Object> entry : cv.valueSet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                cvString.append(key).append(": ").append(value).append("\n");
            }

            mCalibrationModel.mCalResText.postValue(cvString.toString());
            mCalDb.updateOrCreate(cv);

            if (mCalibrating) {
                runNextCal();
            }
        }
    };


    // Create the observer which updates the UI.
    final Observer<byte[]> btCharacteristicPositionObserver = new Observer<byte[]>() {

        @Override
        public void onChanged(@Nullable final byte[] value) {
            FeyiuState.getInstance().update(value);

            binding.textGimbalStatus.setText(
                    FeyiuState.getInstance().angle_tilt.posToString() + " | " +
                            FeyiuState.getInstance().angle_pan.posToString() + " | " +
                            FeyiuState.getInstance().angle_yaw.posToString());

            if (mCalRunnable != null) {
                mCalRunnable.onGimbalUpdate();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}