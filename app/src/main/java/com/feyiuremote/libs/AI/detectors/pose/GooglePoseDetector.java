package com.feyiuremote.libs.AI.detectors.pose;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.abstracts.IObjectDetector;
import com.feyiuremote.libs.AI.detectors.abstracts.IObjectDetectorListener;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GooglePoseDetector implements IObjectDetector {

    public final static String TAG = GooglePoseDetector.class.getSimpleName();
    private static final boolean USE_GPU = false;

    private ExecutorService executor;

    private PoseDetector mPoseDetector;

    private boolean mIsProcessing = false;

    private Bitmap frameBitmap;
    private IPoseDetectorListener listener;

    public GooglePoseDetector() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void init(Context context) {
        this.mPoseDetector = PoseDetection.getClient(getPoseDetectorOptions());
    }

    private PoseDetectorOptionsBase getPoseDetectorOptions() {
        PoseDetectorOptions.Builder builder = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE);

        if (USE_GPU) {
            builder.setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU);
        }

        return builder.build();
    }

    @Override
    public void setListener(IObjectDetectorListener listener) {
        this.listener = (IPoseDetectorListener) listener;
    }

    @Override
    public void onNewFrame(CameraFrame frame) {
        frameBitmap = frame.bitmap();
        InputImage image = InputImage.fromBitmap(frameBitmap, 0);

        if (!mIsProcessing) {
            track(image);
        }
    }

    public void track(InputImage image) {
        executor.execute(() -> {
                    mIsProcessing = true;
            Task<Pose> t = mPoseDetector.process(image);
                    t.addOnSuccessListener(pose -> {
                        mIsProcessing = false;
                        onPoseUpdate(pose);
                    });
                }
        );
    }

    private void onPoseUpdate(Pose pose) {
        listener.onPoseUpdate(pose, frameBitmap);
    }

    @Override
    public void shutdown() {
        this.executor.shutdown();
    }

    @Override
    public void stop() {

    }


}
