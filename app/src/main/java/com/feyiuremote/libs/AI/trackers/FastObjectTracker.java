package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.AI.ObjectUtils;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.concurrent.ExecutorService;

import boofcv.abst.tracker.ConfigTrackerTld;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.android.ConvertBitmap;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Quadrilateral_F64;

public class FastObjectTracker implements IObjectTracker {
    private final String TAG = FastObjectTracker.class.getSimpleName();
    public final static String TRACKER_CIRCULANT = "Circulant";
    public final static String TRACKER_TLD = "TLD";

    private ExecutorService executor;

    private Mat mImageMatrix;

    private TrackerObjectQuad<GrayU8> mTracker;

    private boolean mIsProcessing = false;
    private boolean mInitIsPending = false;

    private GrayU8 mTrackingImage;
    private Quadrilateral_F64 mPOIasPolygon;
    private POI mPOI;
    private IObjectTrackerListener mListener;

    public FastObjectTracker(ExecutorService executor) {
        this(executor, TRACKER_CIRCULANT);
    }

    public FastObjectTracker(ExecutorService executor, String tracker_type) {
        this.executor = executor;

        if (tracker_type.equals(TRACKER_CIRCULANT)) {
            mTracker = FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
        } else if (tracker_type.equals(TRACKER_TLD)) {
            mTracker = FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(false), GrayU8.class);
        }
    }

    public void setUpdateListener(IObjectTrackerListener listener) {
        this.mListener = listener;
    }

    public void onNewFrame(Bitmap bitmap) {
        int bitmapW = bitmap.getWidth();
        int bitmapH = bitmap.getHeight();
        // Sometimes bitmap is null
        mImageMatrix = new Mat(bitmapH, bitmapW, CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mImageMatrix);

        // TODO: Might be better perfomance if wouldnt do the resizing
        mTrackingImage = new GrayU8(bitmapW, bitmapH);
        ConvertBitmap.bitmapToBoof(bitmap, mTrackingImage, null);

        if (isLocked()) {
            if (!mIsProcessing) {
                track();
            }

            if (this.mListener != null && mPOI != null) {
                executor.execute(() -> mListener.onUpdate(mPOI));
            }
        }
    }

    public void lock(POI mPoi) {
        this.mPOI = mPoi;
        mPOIasPolygon = mPoi.toPolygon();
        mInitIsPending = true;
    }

    @Override
    public void cancel() {
        Log.d(TAG, "Tracking cancelled");
        mPOI = null;
    }

    private void track() {
        if (isLocked()) {
            this.executor.execute(() -> {
                mIsProcessing = true;

                try {
                    if (mInitIsPending) {
                        mTracker.initialize(mTrackingImage, mPOIasPolygon);
                        mInitIsPending = false;
                    } else {
                        mTracker.process(mTrackingImage, mPOIasPolygon);
                        mPOI.update(mPOIasPolygon); // Update POI location
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Tracking image size has changed!");
                    cancel();
                } catch (NullPointerException e) {
                    Log.e(TAG, "Tracking polygon is null!");
                } catch (ArrayIndexOutOfBoundsException e) {
                    mInitIsPending = true;
                    Log.e(TAG, "Could apply FFT, probably something wrong with image dimensions!");
                }

                mIsProcessing = false;
            });
        }
    }

    public POI getPOI() {
        return mPOI;
    }

    public boolean isLocked() {
        return mPOI != null;
    }

}
