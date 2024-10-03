package com.feyiuremote.ui.gimbal;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentGimbalBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.camera.CameraFragment;
import com.feyiuremote.ui.gimbal.adapters.BluetoothScanResultsAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class GimbalFragment extends Fragment {

    private final static String TAG = GimbalFragment.class.getSimpleName();

    private final static String GIMBAL_MAC = "80:7D:3A:FE:84:36";
    private FragmentGimbalBinding binding;

    private BluetoothScanResultsAdapter mScanResultListAdapter;

    private BluetoothViewModel mBluetoothViewModel;

    private MainActivity mainActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        mainActivity = (MainActivity) getActivity();
        binding = FragmentGimbalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView mTextStatus = binding.textStatus;
        mBluetoothViewModel.statusMessage.observe(getViewLifecycleOwner(), mTextStatus::setText);
        mBluetoothViewModel.connectionStatus.observe(getViewLifecycleOwner(), btConnectionStatusObserver);
        mBluetoothViewModel.scanResults.observe(getViewLifecycleOwner(), scanResultsObserver);

        final Button mButtonConnect = binding.buttonConnect;
        mButtonConnect.setOnClickListener(view -> {
            connectToGimbal();
        });

        final ListView mListView = binding.listScanResults;
        mScanResultListAdapter = new BluetoothScanResultsAdapter(getContext());
        mListView.setAdapter(mScanResultListAdapter);

        mBluetoothViewModel.registerCharacteristic(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID);
        mBluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        return root;
    }

    private void switchToCamera() {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_camera,
                null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_gimbal, true)
                        .build());
    }

    final Observer<String> btConnectionStatusObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable final String status) {
            switch (status) {
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                case BluetoothLeService.ACTION_GATT_CONNECTION_FAILED:
                    binding.buttonConnect.setEnabled(false);
                    connectToGimbal();
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTING:
                    binding.buttonConnect.setEnabled(false);
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    binding.buttonConnect.setEnabled(true);
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTING:
                    binding.buttonConnect.setEnabled(false);
                    break;
                case BluetoothLeService.ACTION_BT_ENABLED:
                    binding.buttonConnect.setEnabled(false);
                    connectToGimbal();
                    break;
            }
        }
    };

    private void connectToGimbal() {
        if (mainActivity.mBluetoothLeService != null) {
            if (mBluetoothViewModel.connected.getValue()) {
                mainActivity.mBluetoothLeService.disconnect();
            } else {
                mainActivity.mBluetoothLeService.connect(GIMBAL_MAC);
                Log.d(TAG, "Connecting to gimbal");
            }
        }
    }

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

            Log.d(TAG, "Switching to Camera...");

            if (mBluetoothViewModel.connected.getValue()) {
                switchToCamera();
            }
        }
    };


    // Create the observer which updates the UI.
    final Observer<ArrayList<ScanResult>> scanResultsObserver = new Observer<ArrayList<ScanResult>>() {
        @Override
        public void onChanged(@Nullable final ArrayList<ScanResult> newScanResults) {
            mScanResultListAdapter.setResults(newScanResults);
            mScanResultListAdapter.notifyDataSetChanged();

            if (!mBluetoothViewModel.connected.getValue()) {
                connectToGimbal();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}