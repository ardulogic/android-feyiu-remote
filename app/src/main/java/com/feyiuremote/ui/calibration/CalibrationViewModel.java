package com.feyiuremote.ui.calibration;

import android.content.ContentValues;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CalibrationViewModel extends ViewModel {

    public final MutableLiveData<String> mPosText = new MutableLiveData<>();
    public final MutableLiveData<String> mCalResText = new MutableLiveData<>();

    public final MutableLiveData<Integer> mProgressJoySens = new MutableLiveData<>();
    public final MutableLiveData<String> mTextJoySens = new MutableLiveData<>();
    public final MutableLiveData<Integer> mProgressJoyVal = new MutableLiveData<>();
    public final MutableLiveData<String> mTextJoyVal = new MutableLiveData<>();

    public final MutableLiveData<String> mTextProgress = new MutableLiveData<>();
    public final MutableLiveData<Integer> mPercProgress = new MutableLiveData<>();
    public final MutableLiveData<ContentValues> mCalibrationCv = new MutableLiveData<>();

    public CalibrationViewModel() {
        mPosText.setValue("Waiting for position...");
        mCalResText.setValue("Waiting for calibration start...");
        mProgressJoySens.setValue(0);
        mTextJoySens.setValue("Joystick Sensitivity");
        mProgressJoyVal.setValue(0);
        mTextJoySens.setValue("Joystick Value");
        mTextProgress.setValue("0%");
        mPercProgress.setValue(0);
        mCalibrationCv.setValue(new ContentValues());
    }

    public LiveData<String> getPosText() {
        return mPosText;
    }

    public LiveData<String> getCalResultText() {
        return mCalResText;
    }

    public LiveData<ContentValues> getCalResultCv() {
        return mCalibrationCv;
    }
}