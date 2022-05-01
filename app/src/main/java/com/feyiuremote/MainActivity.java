package com.feyiuremote;

import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.feyiuremote.libs.AI.FaceDetector;
import com.feyiuremote.libs.Bluetooth.BluetoothIntent;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothModel;
import com.feyiuremote.libs.Bluetooth.BluetoothPermissions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.feyiuremote.databinding.ActivityMainBinding;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_DATA;
import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_ID;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothModel bluetoothModel;
    private ActivityMainBinding binding;
    public BluetoothLeService mBluetoothLeService;
    public ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        bluetoothModel = new ViewModelProvider(this).get(BluetoothModel.class);
        startBtService();
        registerReceiver(mGattUpdateReceiver, BluetoothIntent.getFilter());

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wm.createMulticastLock("multicastLock");
        multicastLock.acquire();

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        FaceDetector.init(this);
    }

    private void startBtService() {
        if (!BluetoothPermissions.check(this)) {
            BluetoothPermissions.request(this);
        } else {
            Log.d(TAG, "Starting bluetooth service...");
            startService(new Intent(this, BluetoothLeService.class));
            Intent serviceIntent = new Intent(this, BluetoothLeService.class);
            bindService(serviceIntent, mBtServiceConnection, BIND_AUTO_CREATE);
        }
    }

    /**
     * Manage bluetooth service
     */
    private final ServiceConnection mBtServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "Bluetooth service has been started");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.scan();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothLeService.ACTION_BT_DISABLED:
                    Log.e(TAG, "Bluetooth disabled!");
                    bluetoothModel.mStatus.setValue("Bluetooth is disabled!");
                    bluetoothModel.mConnected.setValue(false);
                    bluetoothModel.mServicesDiscovered.setValue(false);
                    break;

                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    Log.d(TAG, "Connected!");
                    bluetoothModel.mStatus.setValue("Connected.");
                    bluetoothModel.mConnected.setValue(true);
                    bluetoothModel.mServicesDiscovered.setValue(false);
                    break;

                case BluetoothLeService.ACTION_GATT_CONNECTING:
                    Log.d(TAG, "Connected!");
                    bluetoothModel.mStatus.setValue("Connecting...");
                    bluetoothModel.mConnected.setValue(false);
                    bluetoothModel.mServicesDiscovered.setValue(false);
                    break;

                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    Log.d(TAG, "Disconnected!");
                    bluetoothModel.mStatus.setValue("Disconnected.");
                    bluetoothModel.mConnected.setValue(false);
                    bluetoothModel.mServicesDiscovered.setValue(false);
                    break;

                case BluetoothLeService.ACTION_SCAN_RESULTS:
                    Log.d(TAG, "Scan results updated");
                    ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(EXTRA_DATA);
                    Log.d(TAG, scanResults.toString());
                    bluetoothModel.mStatus.postValue("Scan results have been updated");
                    bluetoothModel.mScanResults.postValue(scanResults);
                    break;

                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    Log.d(TAG, "Services discovered");
                    bluetoothModel.mStatus.postValue("Services have been discovered");
                    bluetoothModel.mServicesDiscovered.setValue(true);
                    break;

                case BluetoothLeService.ACTION_GATT_CHARACTERISTIC_UPDATE:
                    String id = intent.getStringExtra(EXTRA_ID);
                    if (bluetoothModel.mCharacteristics.containsKey(id)) {
                        bluetoothModel.mCharacteristics.get(id).setValue(intent.getByteArrayExtra(EXTRA_DATA));
                    } else {
                        bluetoothModel.mCharacteristics.put(id, new MutableLiveData<byte[]>(intent.getByteArrayExtra(EXTRA_DATA)));
                    }

                    Log.d(TAG, "Characteristic updated:" + intent.getByteArrayExtra(EXTRA_DATA).toString());
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);

        startBtService();
    }

}