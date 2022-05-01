package com.feyiuremote.ui.home;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentHomeBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothModel;
import com.feyiuremote.libs.Bluetooth.BluetoothPermissions;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private final static String TAG = HomeFragment.class.getSimpleName();

    private FragmentHomeBinding binding;

    private ScanListAdapter mScanResultListAdapter;

    private HomeViewModel mHomeViewModel;
    private BluetoothModel mBluetoothViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mHomeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView mTextStatus = binding.textStatus;
        mBluetoothViewModel.mStatus.observe(getViewLifecycleOwner(), mTextStatus::setText);
        mBluetoothViewModel.mConnected.observe(getViewLifecycleOwner(), btConnectionObserver);

        final Button mButtonConnect = binding.buttonConnect;
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.mBluetoothLeService.disconnect();
                mainActivity.mBluetoothLeService.connect("80:7D:3A:FE:84:36");
            }
        });

        binding.buttonMoveLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                        FeyiuUtils.move(-binding.seekBarX.getProgress() - 50, -binding.seekBarY.getProgress() - 50)
                );            }
        });

        binding.buttonMoveRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity mainActivity = (MainActivity) getActivity();

                mainActivity.mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                        FeyiuUtils.move(binding.seekBarX.getProgress() + 50, binding.seekBarY.getProgress() + 50)
                );
            }
        });

        final ListView mListView = binding.listScanResults;
        mScanResultListAdapter = new ScanListAdapter(getContext());
        mListView.setAdapter(mScanResultListAdapter);

        mBluetoothViewModel.mServicesDiscovered.observe(getViewLifecycleOwner(), btServicesObserver);
        mBluetoothViewModel.registerCharacteristic(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID);
        mBluetoothViewModel.mCharacteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), btCharacteristicPositionObserver);

        return root;
    }

    final Observer<Boolean> btConnectionObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable final Boolean connected) {
            if (connected) {
                binding.buttonConnect.setText("Reconnect");
            } else {
                binding.buttonConnect.setText("Connect");
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

                    mBluetoothViewModel.mStatus.postValue("Successfully subscribed to characteristic");
                } else {
                    mBluetoothViewModel.mStatus.postValue("Gatt service is not yet available");
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
                    FeyiuState.getInstance().pos_tilt.getValue().toString() + " | " +
                            FeyiuState.getInstance().pos_pan.getValue().toString() + " | " +
                            FeyiuState.getInstance().pos_yaw.getValue().toString());
        }
    };


    public class ScanListAdapter extends BaseAdapter {

        Context c;
        ArrayList<ScanResult> results;

        public ScanListAdapter(Context c) {
            this.c = c;
            mBluetoothViewModel.mScanResults.observe(getViewLifecycleOwner(), scanResultsObserver);
            this.results = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public ScanResult getItem(int i) {
            return this.results.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(c).inflate(R.layout.fragment_home_scan_item, viewGroup, false);
            }

            TextView mTextDeviceTitle = (TextView) view.findViewById(R.id.textDeviceTitle);
            TextView mTextDeviceAddress = (TextView) view.findViewById(R.id.textDeviceAddress);

            mTextDeviceTitle.setText(this.results.get(i).getScanRecord().getDeviceName() != null
                    ? this.results.get(i).getScanRecord().getDeviceName() :
                    "Unknown");
            mTextDeviceAddress.setText(this.results.get(i).getDevice().getAddress());

            return view;
        }

        // Create the observer which updates the UI.
        final Observer<ArrayList<ScanResult>> scanResultsObserver = new Observer<ArrayList<ScanResult>>() {
            @Override
            public void onChanged(@Nullable final ArrayList<ScanResult> newScanResults) {
                results = newScanResults;
                mScanResultListAdapter.notifyDataSetChanged();
            }
        };

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}