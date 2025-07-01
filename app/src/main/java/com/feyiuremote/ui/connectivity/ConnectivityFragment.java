package com.feyiuremote.ui.connectivity;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentConnectivityBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothPreferencesManager;
import com.feyiuremote.libs.Bluetooth.BluetoothSetupDialog;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Utils.WifiConnector;
import com.feyiuremote.libs.WiFi.WifiViewModel;
import com.feyiuremote.ui.calibration.CalibrationFragment;

import java.util.ArrayList;

public class ConnectivityFragment extends Fragment {
    private final static String TAG = ConnectivityFragment.class.getSimpleName();

    private FragmentConnectivityBinding binding;
    private BluetoothViewModel mBluetoothViewModel;
    private MainActivity mainActivity;
    private WifiViewModel mWifiViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mainActivity = (MainActivity) getActivity();
        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        binding = FragmentConnectivityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView mTextStatus = binding.textStatus;
        mBluetoothViewModel.statusMessage.observe(getViewLifecycleOwner(), mTextStatus::setText);
        mBluetoothViewModel.connectionStatus.observe(getViewLifecycleOwner(), btConnectionStatusObserver);
        mBluetoothViewModel.scanResults.observe(getViewLifecycleOwner(), bluetoothScanResultsObserver);
        mBluetoothViewModel.ssid.observe(getViewLifecycleOwner(), bluetoothConnectivitySsidObserver);
        mBluetoothViewModel.feyiuStateUpdated.observe(getViewLifecycleOwner(), mFeyiuStateObserver);

        binding.buttonConnectGimbal.setOnClickListener(view -> {
            connectToGimbal();
        });

        binding.buttonBluetoothSetup.setOnClickListener(v -> {
            BluetoothSetupDialog dialogManager = new BluetoothSetupDialog(getContext(), mBluetoothViewModel);
            dialogManager.showSetupOptionsDialog();
        });

//        binding.buttonEmulateGimbal.setOnClickListener(v -> {
//            GimbalEmulator.init(mBluetoothViewModel);
//            GimbalEmulator.enable();
//        });

        mWifiViewModel = new ViewModelProvider(requireActivity()).get(WifiViewModel.class);
        mWifiViewModel.connectionStatus.observe(getViewLifecycleOwner(), wifiConnectionStatusObserver);
        mWifiViewModel.ssid.observe(getViewLifecycleOwner(), wifiConnectionSsidObserver);

        binding.buttonWifiSetup.setOnClickListener(v -> {
            mWifiViewModel.getWifiConnector().editAPs(mainActivity);
        });

        binding.buttonConnectCamera.setOnClickListener(v -> {
            mWifiViewModel.getWifiConnector().askToChooseAP(mainActivity);
        });

        if (!CalibrationFragment.isCalibrated()) {
            binding.textGimbalImageStatus.setText("Not Calibrated!");
            binding.imageGimbalStatus.setImageResource(R.drawable.warning);
        } else {
            binding.textGimbalImageStatus.setText("Calibration OK.");
            binding.imageGimbalStatus.setImageResource(R.drawable.thumbs_up);
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    final Observer<String> wifiConnectionSsidObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String ssid) {
            binding.textConnectivityCameraSsid.setText(ssid);
        }
    };

    final Observer<String> wifiConnectionStatusObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String status) {
            if (status.equals(WifiConnector.CONNECTED)) {
                binding.buttonConnectCamera.setEnabled(true);
                binding.buttonConnectCamera.setText("Connected");
            } else if (status.equals(WifiConnector.CONNECTING) ||
                    status.equals(WifiConnector.DISCONNECTING)) {
                binding.buttonConnectCamera.setEnabled(false);
                binding.buttonConnectCamera.setText("Connect");
            } else {
                binding.buttonConnectCamera.setEnabled(true);
                binding.buttonConnectCamera.setText("Connect");
            }
        }
    };

    private void switchToCamera() {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_camera,
                null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_connectivity, true)
                        .build());
    }

    private void switchToCalibration() {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_calibration,
                null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_calibration, true)
                        .build());
    }

    final Observer<String> btConnectionStatusObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable final String status) {
            switch (status) {
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                case BluetoothLeService.ACTION_GATT_CONNECTION_FAILED:
                    binding.buttonConnectGimbal.setText("Disconnected");
                    connectToGimbal();
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTING:
                    binding.buttonConnectGimbal.setText("Connecting");
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    binding.buttonConnectGimbal.setText("Connected");
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTING:
                    binding.buttonConnectGimbal.setText("Disconnecting");
                    break;
                case BluetoothLeService.ACTION_BT_ENABLED:
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
                BluetoothPreferencesManager manager = new BluetoothPreferencesManager(getContext());
                String mac = manager.getActiveMac();
                if (mac != null) {
                    mainActivity.mBluetoothLeService.connect(mac);
                    Log.d(TAG, "Connecting to gimbal: " + mac);
                } else {
                    Log.e(TAG, "Cam't connect, mac is null!");
                }
            }
        }
    }

    /**
     * Observer for Gimbal state updates
     */
    final Observer<Long> mFeyiuStateObserver = new Observer<Long>() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onChanged(Long timestamp) {
            final TextView mTextPosition = binding.textPosition;

            mTextPosition.setText(
                    FeyiuState.getInstance().angle_tilt.posToString() + " | " +
                            FeyiuState.getInstance().angle_pan.posToString() + " | " +
                            FeyiuState.getInstance().angle_yaw.posToString());

//            Log.d(TAG, "Switching to Camera...");
//            switchToCamera();

            if (mBluetoothViewModel.connected.getValue()) {
                if (!CalibrationFragment.isCalibrated()) {
                    switchToCalibration();
                }
            }
        }
    };

    final Observer<String> bluetoothConnectivitySsidObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable final String ssid) {
            binding.textConnectivityGimbalSsid.setText(ssid);
        }
    };

    // Create the observer which updates the UI.
    final Observer<ArrayList<ScanResult>> bluetoothScanResultsObserver = new Observer<ArrayList<ScanResult>>() {
        @Override
        public void onChanged(@Nullable final ArrayList<ScanResult> newScanResults) {
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