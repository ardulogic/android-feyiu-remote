package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.ObjectUtils;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutorService;

import boofcv.abst.tracker.ConfigTrackerTld;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.android.ConvertBitmap;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Quadrilateral_F64;

public class BCVObjectTracker {

    public final static String TRACKER_CIRCULANT = "Circulant";
    public final static String TRACKER_TLD = "TLD";

    private final int TRACKING_RES_WIDTH = 640;
    private final int TRACKING_RES_HEIGHT = 480;
    private ExecutorService executor;
    private RectangleDrawView mRectDrawView;

    private Mat mImageMatrix;

    private TrackerObjectQuad<GrayU8> mTracker;
    private Integer mAreaW;
    private Integer mAreaH;

    private Rect mTrackingIntRectangle;
    private Rect mTrackingExtRectangle;

    private boolean mIsProcessing = false;
    private boolean mInitIsPending = false;

    private GrayU8 mTrackingImage;
    private Quadrilateral_F64 mTrackingIntPoly;
    private IPoiUpdateListener mPoiUpdateListener;

    public BCVObjectTracker(RectangleDrawView rectDrawView, ExecutorService executor) {
        this(TRACKER_CIRCULANT, rectDrawView);
        this.executor = executor;
    }

    public BCVObjectTracker(String tracker, RectangleDrawView rectangleDrawView) {
        this.mRectDrawView = rectangleDrawView;

        if (tracker.equals(TRACKER_CIRCULANT)) {
            mTracker = FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
        } else if (tracker.equals(TRACKER_TLD)) {
            mTracker = FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(false), GrayU8.class);
        }
    }

    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
        this.mPoiUpdateListener = listener;
    }

    public Bitmap onNewFrame(Bitmap bitmap) {
        mImageMatrix = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mImageMatrix);

        mTrackingImage = new GrayU8(TRACKING_RES_WIDTH, TRACKING_RES_HEIGHT);
        ConvertBitmap.bitmapToBoof(bitmap, mTrackingImage, null);

        if (isLocked()) {
            if (!mIsProcessing) {
                track();
            }

            bitmap = this.drawTrackingRectOnInput();

            if (this.mPoiUpdateListener != null && mTrackingIntRectangle != null) {
                this.executor.execute(new mPoiUpdateRunnable(bitmap, mTrackingIntRectangle));
            }
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
        mTrackingIntPoly = ObjectUtils.rectToPolygon(mTrackingIntRectangle);

        mInitIsPending = true;
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

                    if (mInitIsPending) {
                        mTracker.initialize(mTrackingImage, mTrackingIntPoly);
                        mInitIsPending = false;
                    } else {
                        mTracker.process(mTrackingImage, mTrackingIntPoly);
                        mTrackingIntRectangle = ObjectUtils.polygonToRect(mTrackingIntPoly);
                    }

                    mIsProcessing = false;
                }
            });
        }
    }

    public Bitmap drawTrackingRectOnInput() {
        Mat im = mImageMatrix.clone();
        Imgproc.rectangle(im, ObjectUtils.polygonToRect(mTrackingIntPoly), new Scalar(0, 255, 0), 3);

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

    public boolean isLocked() {
        return mTrackingIntRectangle != null;
    }

}
