package com.feyiuremote.ui.notifications;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentNotificationsBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationRunnable;
import com.feyiuremote.libs.Feiyu.calibration.ICalibrationListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private MainActivity mainActivity;
    private BluetoothModel mBluetoothViewModel;
    private boolean mCharacteristicReady;
    private CalibrationDbHelper mCalDb;
    private CalibrationPresetDbHelper mCalPresetDb;
    private CalibrationRunnable mCalRunnable;
    private NotificationsViewModel mNotificationsModel;

    private static HashMap<Integer, int[]> cal_map = new HashMap<Integer, int[]>(){{
        put(25, new int[]{65, 90, 100, 105, 135, 170, 200, 205, 235, 240});
        put(60, new int[]{55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225, 230, 235, 240});
        put(230, new int[]{52, 53, 54, 55, 56, 57, 58, 60, 61, 62, 64, 66, 68, 70, 71, 72, 74, 76, 78, 79, 80, 82, 83, 85, 90, 100, 110, 130, 140});
    }};

    private int i_joy_val = 0;
    private int i_joy_sen = 0;
    private int i_dir = 0;
    private boolean mCalibrating = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mNotificationsModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView posTextView = binding.textPos;
        mNotificationsModel.getPosText().observe(getViewLifecycleOwner(), posTextView::setText);

        final TextView calResTextView = binding.textCalResult;
        mNotificationsModel.getCalResultText().observe(getViewLifecycleOwner(), calResTextView::setText);

        mainActivity = (MainActivity) getActivity();

        mCalDb = new CalibrationDbHelper(mainActivity);
        mCalPresetDb = new CalibrationPresetDbHelper(mainActivity);

        mBluetoothViewModel.mCharacteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
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
        i_joy_sen = 0;
        i_joy_val = 0;

        mCalRunnable = new CalibrationRunnable(
                getCalJoySens(),
                getCalJoyVal(),
                mainActivity.mBluetoothLeService,
                mCalibrationListener
        );

        mainActivity.executor.execute(mCalRunnable);
        mCalibrating = true;
    }

    private int getCalJoySens() {
        Object[] keys = cal_map.keySet().toArray();
        return (int) keys[i_joy_sen];
    }

    private int getCalJoyVal() {
        return cal_map.get(getCalJoySens())[i_joy_val];
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
                return;
            }

            mCalRunnable = new CalibrationRunnable(
                    getCalJoySens(),
                    getCalJoyVal(),
                    mainActivity.mBluetoothLeService,
                    mCalibrationListener
            );

            i_dir = 1;
        } else {
            mCalRunnable = new CalibrationRunnable(
                    getCalJoySens(),
                    -getCalJoyVal(),
                    mainActivity.mBluetoothLeService,
                    mCalibrationListener
            );

            i_dir = 0;
        }

        mainActivity.executor.execute(mCalRunnable);
    }

    private void saveCalibrationAsPreset() {
        mCalPresetDb.deleteAll();

        for(Map.Entry<Integer, int[]> entry : cal_map.entrySet()) {
            int joy_sen = entry.getKey();
            int[] joy_vals = entry.getValue();
            for (int joy_val : joy_vals) {
                ArrayList<ContentValues> rows = mCalDb.getByJoyState(joy_sen, joy_val);

                for (ContentValues row : rows) {
                    mCalPresetDb.create(row);
                }
            }
        }

        mNotificationsModel.mCalResText.postValue("Calibration saved!");
    }

    private ICalibrationListener mCalibrationListener = new ICalibrationListener() {
        @Override
        public void onCalFinished(ContentValues cv) {
            mNotificationsModel.mCalResText.postValue(cv.toString());
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

            final TextView mTextPosition = binding.textPos;

            mTextPosition.setText(
                    FeyiuState.getInstance().pos_tilt.getValue().toString() + " | " +
                            FeyiuState.getInstance().pos_pan.getValue().toString() + " | " +
                            FeyiuState.getInstance().pos_yaw.getValue().toString());

            if (mCalRunnable != null) {
                mCalRunnable.characteristicTick();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}