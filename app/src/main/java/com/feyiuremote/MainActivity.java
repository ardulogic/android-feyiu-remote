package com.feyiuremote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.feyiuremote.databinding.ActivityMainBinding;
import com.feyiuremote.libs.AI.detectors.FaceDetector;
import com.feyiuremote.libs.Bluetooth.BluetoothIntent;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Bluetooth.BluetoothLeUpdateReceiver;
import com.feyiuremote.libs.Bluetooth.BluetoothPermissions;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Bluetooth.IOnBluetoothServicesDiscovered;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.opencv.android.OpenCVLoader;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothViewModel bluetoothViewModel;
    private ActivityMainBinding binding;

    private Boolean locked = false;

    public BluetoothLeService mBluetoothLeService;
    public ExecutorService executor = Executors.newFixedThreadPool(10);

    static {
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            System.loadLibrary("opencv_java4");
        }
    }

    private PowerManager.WakeLock mWakeLock;
    private BluetoothLeUpdateReceiver mGattUpdateReceiver;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This needs to be available before inflating fragments
        CalibrationDB.init(this);

        // Hide the status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_gimbal, R.id.navigation_camera, R.id.navigation_calibration)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        bluetoothViewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        startBtService();

        mGattUpdateReceiver = new BluetoothLeUpdateReceiver(bluetoothViewModel);
        mGattUpdateReceiver.setOnConnectedListener(mBluetoothServicesDiscoveredListener);
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

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "feyiuremote:main");
        this.mWakeLock.acquire();

        ensureLocationEnabled();
        ensureBluetoothEnabled();
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

    private IOnBluetoothServicesDiscovered mBluetoothServicesDiscoveredListener = new IOnBluetoothServicesDiscovered() {
        @Override
        public void onServicesDiscovered() {
            BluetoothGattService gattService = mBluetoothLeService.getService(FeyiuUtils.SERVICE_ID);
            if (gattService != null) {
                BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID));
                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
            }
        }
    };

    public void ensureLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is not enabled, prompt the user to enable it
            buildAlertMessageNoGps();
        }
    }

    public void ensureBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled, prompt the user to enable it
            buildAlertMessageNoBluetooth();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    // Open location settings
                    Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(enableGpsIntent, 1);
                    mBluetoothLeService.scan();
                })
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @SuppressLint("MissingPermission")
    private void buildAlertMessageNoBluetooth() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Bluetooth seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    // Open Bluetooth settings
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, 2);
                    // Start Bluetooth scanning or any other Bluetooth-related operations
                    // Replace the following line with your Bluetooth-related code
                    // mBluetoothLeService.scan();
                })
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 401) {
            if (!locked) {
                lockAppScreen();
            } else {
                unlockAppScreen();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void lockAppScreen() {
        View decorView = getWindow().getDecorView();

        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);

        binding.imageLockScreenButton.setVisibility(View.VISIBLE);
        locked = true;
    }

    private void unlockAppScreen() {
        View decorView = getWindow().getDecorView();

        // Show both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;

        decorView.setSystemUiVisibility(uiOptions);

        binding.imageLockScreenButton.setVisibility(View.GONE);
        locked = false;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);

        startBtService();
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        mBluetoothLeService.disconnect();

        super.onDestroy();
    }

}