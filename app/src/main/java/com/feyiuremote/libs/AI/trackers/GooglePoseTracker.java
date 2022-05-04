package com.feyiuremote.libs.AI.trackers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;

import com.feyiuremote.libs.AI.ObjectUtils;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.interfaces.Detector;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutorService;

public class GooglePoseTracker {

    public final static String TAG = GooglePoseTracker.class.getSimpleName();
    private static final boolean USE_GPU = false;

    private final int TRACKING_RES_WIDTH = 640;
    private final int TRACKING_RES_HEIGHT = 480;
    private final Context mContext;
    private ExecutorService executor;

    private Rect mTrackingExtRectangle;
    private final PoseDetector mPoseDetector;

    private boolean mIsProcessing = false;

    private Bitmap mTrackingImage;
    private IPoiUpdateListener mPoiUpdateListener;
    private Pose mPose;

    public GooglePoseTracker(Context c, ExecutorService executor) {
        this.mContext = c;
        this.executor = executor;
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

    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
        this.mPoiUpdateListener = listener;
    }

    public Bitmap onNewFrame(Bitmap bitmap) {
//        mTrackingImage = bitmap;
        mTrackingImage = bitmap.createScaledBitmap(bitmap, TRACKING_RES_WIDTH, TRACKING_RES_HEIGHT, false);
        bitmap = mTrackingImage;

        if (!mIsProcessing) {
            track();
        }

        bitmap = this.drawTrackingRect(bitmap);

        if (this.mPoiUpdateListener != null && mTrackingExtRectangle != null) {
            this.executor.execute(new mPoiUpdateRunnable(bitmap, mTrackingExtRectangle));
        }

        return bitmap;
    }

    public void track() {
        executor.execute(() -> {
                    mIsProcessing = true;
                    Task<Pose> t = mPoseDetector.process(mTrackingImage, Detector.TYPE_POSE_DETECTION);
                    t.addOnSuccessListener(pose -> {
                        mIsProcessing = false;
                        mPose = pose;
                    });
                }
        );
    }

    public Bitmap drawTrackingRect(Bitmap bitmap) {
        if (mPose != null) {
            Mat mImageMatrix = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bitmap, mImageMatrix);

            PoseLandmark pNose = mPose.getPoseLandmark(PoseLandmark.NOSE);
            PoseLandmark pLeftShoulder = mPose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark pRightShoulder = mPose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

            float wRatio = (float) bitmap.getWidth() / mTrackingImage.getWidth();
            float hRatio = (float) bitmap.getHeight() / mTrackingImage.getHeight();

            if (pNose != null) {
                PointF position = pNose.getPosition();
                mImageMatrix = drawSmallRectOnPosition(mImageMatrix, position, wRatio, hRatio);

                mTrackingExtRectangle = new Rect((int) position.x - 10, (int) position.y - 10, 20, 20);
                mTrackingExtRectangle = ObjectUtils.transformRect(mTrackingExtRectangle, wRatio, hRatio);
            } else {
                mTrackingExtRectangle = null;
            }

            if (pLeftShoulder != null) {
                mImageMatrix = drawSmallRectOnPosition(mImageMatrix, pLeftShoulder.getPosition(), wRatio, hRatio);
            }

            if (pRightShoulder != null) {
                mImageMatrix = drawSmallRectOnPosition(mImageMatrix, pRightShoulder.getPosition(), wRatio, hRatio);
            }

            Bitmap bitmapOut = Bitmap.createBitmap(mImageMatrix.cols(), mImageMatrix.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImageMatrix, bitmapOut);

            return bitmapOut;
        }

        return bitmap;
    }

    private Mat drawSmallRectOnPosition(Mat m, PointF p, float wRatio, float hRatio) {
        Rect r = new Rect((int) p.x - 10, (int) p.y - 10, 20, 20);

        if (wRatio != 1 || hRatio != 1) {
            r = ObjectUtils.transformRect(r, wRatio, hRatio);
        }

        Imgproc.rectangle(m, r, new Scalar(0, 255, 0), 3);

        return m;
    }

    private class mPoiUpdateRunnable implements Runnable {

        private final Bitmap poiBitmap;
        private final Rect poiRect;

        public mPoiUpdateRunnable(Bitmap bitmap, Rect poiRect) {
            super();

            this.poiBitmap = bitmap;
            this.poiRect = poiRect;
        }

        @Override
        public void run() {
            mPoiUpdateListener.onPoiUpdate(poiBitmap, poiRect);
        }
    }

}
