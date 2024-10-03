package com.feyiuremote.ui.camera.observers;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;

import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class BluetoothEnabledObserver implements Observer<Boolean> {
    private final MainActivity mainActivity;
    private static Boolean prevValue = null;

    public BluetoothEnabledObserver(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        prevValue = null;
    }

    @Override
    public void onChanged(Boolean enabled) {
        if (!enabled && prevValue != enabled) {
            mainActivity.ensureBluetoothEnabled();
            prevValue = enabled;

            navigateToGimbal();
        }

    }

    private void navigateToGimbal() {
        NavController navController = Navigation.findNavController(mainActivity, R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_gimbal,
                null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_camera, true)
                        .build());
    }

}
