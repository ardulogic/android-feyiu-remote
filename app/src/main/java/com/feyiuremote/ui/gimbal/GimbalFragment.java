package com.feyiuremote.ui.gimbal;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentGimbalBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.ui.gimbal.adapters.BluetoothScanResultsAdapter;

import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class GimbalFragment extends Fragment {

    private final static String TAG = GimbalFragment.class.getSimpleName();

    private FragmentGimbalBinding binding;

    private BluetoothScanResultsAdapter mScanResultListAdapter;

    private BluetoothViewModel mBluetoothViewModel;
    private CalibrationDbHelper mDb;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);

        binding = FragmentGimbalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView mTextStatus = binding.textStatus;
        mBluetoothViewModel.status_message.observe(getViewLifecycleOwner(), mTextStatus::setText);
        mBluetoothViewModel.connection_status.observe(getViewLifecycleOwner(), btConnectionStatusObserver);

        final Button mButtonConnect = binding.buttonConnect;
        mButtonConnect.setOnClickListener(view -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.mBluetoothLeService.disconnect();
            mainActivity.mBluetoothLeService.connect("80:7D:3A:FE:84:36");
        });

        final ListView mListView = binding.listScanResults;
        mScanResultListAdapter = new BluetoothScanResultsAdapter(getContext());
        mListView.setAdapter(mScanResultListAdapter);

        mBluetoothViewModel.services_discovered.observe(getViewLifecycleOwner(), btServicesObserver);
        mBluetoothViewModel.registerCharacteristic(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID);
        mBluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        this.mDb = new CalibrationDbHelper(getContext());
        if (mDb.rowCount() < 50) {
            binding.textGimbalImageStatus.setText("Not Calibrated!");
            binding.imageGimbalStatus.setImageResource(R.drawable.warning);
        } else {
            binding.textGimbalImageStatus.setText("Calibration OK.");
            binding.imageGimbalStatus.setImageResource(R.drawable.thumbs_up);
        }

        return root;
    }

    final Observer<String> btConnectionStatusObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable final String status) {
            switch (status) {
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    binding.buttonConnect.setText("Connect");
                    binding.buttonConnect.setEnabled(true);
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTING:
                    binding.buttonConnect.setText("Connecting...");
                    binding.buttonConnect.setEnabled(false);
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    binding.buttonConnect.setText("Reconnect");
                    binding.buttonConnect.setEnabled(true);
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTING:
                    binding.buttonConnect.setText("Disconnecting...");
                    binding.buttonConnect.setEnabled(false);
                    break;
            }
        }
    };

    // Create the observer which updates the UI.
    final Observer<Boolean> btServicesObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable final Boolean servicesDiscovered) {
            if (servicesDiscovered) {
                MainActivity mainActivity = (MainActivity) getActivity();

                BluetoothGattService gattService = mainActivity.mBluetoothLeService.getService(FeyiuUtils.SERVICE_ID);

                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID));
                    mainActivity.mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);

                    mBluetoothViewModel.status_message.postValue("Successfully subscribed to characteristic");
                } else {
                    mBluetoothViewModel.status_message.postValue("Gatt service is not yet available");
                }
            }
        }
    };

    // Create the observer which updates the UI.
    final Observer<byte[]> btCharacteristicPositionObserver = new Observer<byte[]>() {
        @Override
        public void onChanged(@Nullable final byte[] value) {
            FeyiuState.getInstance().update(value);

            final TextView mTextPosition = binding.textPosition;

            mTextPosition.setText(
                    FeyiuState.getInstance().angle_tilt.posToString() + " | " +
                            FeyiuState.getInstance().angle_pan.posToString() + " | " +
                            FeyiuState.getInstance().angle_yaw.posToString());
        }
    };


    // Create the observer which updates the UI.
    final Observer<ArrayList<ScanResult>> scanResultsObserver = new Observer<ArrayList<ScanResult>>() {
        @Override
        public void onChanged(@Nullable final ArrayList<ScanResult> newScanResults) {
            mScanResultListAdapter.setResults(newScanResults);
            mScanResultListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}