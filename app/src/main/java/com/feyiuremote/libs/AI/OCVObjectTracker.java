package com.feyiuremote.libs.AI;

import android.graphics.Bitmap;


import com.feyiuremote.libs.AI.views.RectangleDrawView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Tracker;
import org.opencv.video.TrackerMIL;

import java.util.concurrent.ExecutorService;

public class OCVObjectTracker {

    public final static String TRACKER_MIL = "MIL";
    public final static String TRACKER_DASIAMRPN = "DaSiamRPN";
    public final static String TRACKER_GOTURN = "GOTURN";
    private static final String TRACKER_MOSSE = "Mosse";

    private final int TRACKING_RES_WIDTH = 640;
    private final int TRACKING_RES_HEIGHT = 480;
    private ExecutorService executor;
    private RectangleDrawView mRectDrawView;

    private Mat mImageMatrix;

    private Tracker mTracker;
    private Integer mAreaW;
    private Integer mAreaH;

    private Rect mTrackingIntRectangle;
    private Rect mTrackingExtRectangle;

    private boolean mIsProcessing = false;
    private Mat mImageGrabInit;

    public OCVObjectTracker(RectangleDrawView rectDrawView, ExecutorService executor) {
        this(TRACKER_MIL, rectDrawView);
        this.executor = executor;
    }

    public OCVObjectTracker(String tracker, RectangleDrawView rectangleDrawView) {
        this.mRectDrawView = rectangleDrawView;

        if (tracker.equals(TRACKER_MOSSE)) {
//            mTracker = TrackerMOSSE.create();
        } else if (tracker.equals(TRACKER_GOTURN)) {
//            mTracker = org.opencv.video.TrackerGOTURN.create();
        } else if (tracker.equals(TRACKER_MIL)) {
            mTracker = TrackerMIL.create();
        } else {
//            mTracker.create();
        }
    }

//    public void onNewFrame(byte[] data) {
//        if (!this.isProcessing) {
//            this.mImageMatrix = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
//
////            Core.transpose(mImageMatrix, mImageMatrix);
////            Core.flip(mImageMatrix, mImageMatrix, 1);
////            Imgproc.resize(mImageMatrix, mImageMatrix, new org.opencv.core.Size(,320));
//
//        }
//    }

    public Bitmap onNewFrame(Bitmap bitmap) {
        mImageMatrix = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mImageMatrix);
        Imgproc.resize(mImageMatrix, mImageMatrix, new Size(TRACKING_RES_WIDTH, TRACKING_RES_HEIGHT));

        if (isLocked() && !mIsProcessing) {
            track();
        }

        if (isLocked()) {
            return this.drawTrackingRectOnInput();
        }

        return bitmap;
    }

    public void lock(int x1, int y1, int x2, int y2) {
        updateDrawAreaBounds();

        // Rectangle drawing area is not equal in pixels to the original image
        mTrackingExtRectangle = ObjectUtils.pointsToRect(x1, y1, x2, y2);

        // Image is resized to lower res for tracking
        // we need to account for this
        Float widthRatio = (float) mImageMatrix.cols() / mAreaW;
        Float heightRatio = (float) mImageMatrix.rows() / mAreaH;

        mTrackingIntRectangle = ObjectUtils.transformRect(mTrackingExtRectangle, widthRatio, heightRatio);

        mImageGrabInit = new Mat();
        mImageMatrix.copyTo(mImageGrabInit);
        mTracker.init(mImageGrabInit, mTrackingIntRectangle);
    }

    public void clear() {
        mTrackingIntRectangle = null;
    }

    private void updateDrawAreaBounds() {
        mAreaW = mRectDrawView.getWidth();
        mAreaH = mRectDrawView.getHeight();
    }

    public void track() {
        if (isLocked()) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    mIsProcessing = true;

//                    float widthRatio = (float) mAreaW / mImageMatrix.cols();
//                    float heightRatio = (float) mAreaH / mImageMatrix.rows();
//
//                    mTrackingIntRectangle = ObjectUtils.transformRect(mTrackingExtRectangle, widthRatio, heightRatio);
                    mTracker.update(mImageMatrix, mTrackingIntRectangle);

                    mIsProcessing = false;
                }
            });
        }
    }

    public Bitmap drawTrackingRectOnInput() {
        Mat im = mImageMatrix.clone();
        Imgproc.rectangle(im, mTrackingIntRectangle, new Scalar(0, 255, 0), 3);

        Bitmap bitmapOut = Bitmap.createBitmap(im.cols(), im.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(im, bitmapOut);

        return bitmapOut;
    }

    public Bitmap drawTrackingRect(Bitmap originalBitmap) {
        // Rectangle drawing area is not equal in pixels to the original image
        Float widthRatio = originalBitmap.getWidth() / (float) mAreaW;
        Float heightRatio = originalBitmap.getHeight() / (float) mAreaH;

        Rect imgTrackingRectangle = ObjectUtils.transformRect(mTrackingExtRectangle, widthRatio, heightRatio);

        Mat im = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(originalBitmap, im);
        Imgproc.rectangle(im, imgTrackingRectangle, new Scalar(0, 255, 0), 3);

        Bitmap bitmapOut = Bitmap.createBitmap(im.cols(), im.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(im, bitmapOut);

        return bitmapOut;
    }

    public boolean isLocked() {
        return mTrackingIntRectangle != null;
    }

}
