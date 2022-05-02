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
import com.feyiuremote.libs.Feiyu.calibration.CalibrationRunnable;
import com.feyiuremote.libs.Feiyu.calibration.ICalibrationListener;

import java.math.BigInteger;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private MainActivity mainActivity;
    private BluetoothModel mBluetoothViewModel;
    private boolean mCharacteristicReady;
    private CalibrationDbHelper mDb;
    private CalibrationRunnable mCalRunnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView posTextView = binding.textPos;
        notificationsViewModel.getPosText().observe(getViewLifecycleOwner(), posTextView::setText);

        final TextView calResTextView = binding.textCalResult;
        notificationsViewModel.getCalResultText().observe(getViewLifecycleOwner(), calResTextView::setText);

        mainActivity = (MainActivity) getActivity();

        mDb = new CalibrationDbHelper(mainActivity);

        mBluetoothViewModel.mCharacteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        binding.buttonCalibrate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mCalRunnable = new CalibrationRunnable(
                        binding.seekBarPanSens.getProgress(),
                        55,
                        mainActivity.mBluetoothLeService,
                        new ICalibrationListener() {
                            @Override
                            public void onCalFinished(ContentValues cv) {
                                notificationsViewModel.mCalResText.postValue(cv.toString());
                                mDb.updateOrCreate(cv);
                            }
                        }
                );

                mainActivity.executor.execute(mCalRunnable);
            }
        });

        return root;
    }



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