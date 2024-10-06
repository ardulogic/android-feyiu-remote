package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.Utils.Rectangle;

import java.util.concurrent.ExecutorService;

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.android.ConvertBitmap;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Quadrilateral_F64;

public class FastObjectTracker {
    private final String TAG = FastObjectTracker.class.getSimpleName();
    public final static String TRACKER_CIRCULANT = "Circulant";
    public final static String TRACKER_TLD = "TLD";

    private MainActivity activity;
    private ExecutorService executor;

    private TrackerObjectQuad<GrayU8> mTracker;

    private boolean mIsProcessing = false;
    private boolean mInitIsPending = false;

    private boolean isLocked = false;

    private GrayU8 mFrame;
    private IObjectTrackerListener mListener;

    private POI mPoi;
    private Quadrilateral_F64 trackPolygon;
    private Rectangle trackRectangle;

    public FastObjectTracker(ExecutorService executor, MainActivity a) {
        this.executor = executor;
        this.activity = a;

        ConfigCirculantTracker config = new ConfigCirculantTracker();

        // Set the parameters according to your needs
        // This makes the spatial bandwidth slightly larger,
        // capturing more spatial variations in the target's movement.
        config.output_sigma_factor = 1.0 / 16; // Default 1.0/16

        // A larger bandwidth would make the kernel smoother and less sensitive to minor appearance changes.
        config.sigma = 0.2;

        //For stability in tracking people, you may need a bit more regularization to prevent overfitting to momentary changes
        // (like a person temporarily occluded or partially outside the frame).
        config.lambda = 1e-2;

        // Since people can change appearance relatively quickly (e.g., rotating or taking off a coat),
        // a slightly higher update rate is necessary to allow the model to adjust faster. Recommended: 0.1 - 0.12.
        config.interp_factor = 0.075; // Kernel bandwidth parameter (default is 0.2)

        // People tend to move in and out of regions quickly and their size may change.
        // A padding value of 1 (which doubles the original region) seems good to capture enough context
        // around the person. You could experiment with slightly more, like 1.5,
        // if you're finding that targets are leaving the bounding box too quickly.
        config.padding = 1; // Amount of padding around the target (default is 1.5)


        config.workSpace = 64;

        mTracker = FactoryTrackerObjectQuad.circulant(config, GrayU8.class);

    }

    public void setListener(IObjectTrackerListener listener) {
        this.mListener = listener;
    }

    public synchronized Bitmap onNewFrame(Bitmap bitmap) {
        if (mFrame == null) {
            mFrame = new GrayU8(bitmap.getWidth(), bitmap.getHeight());
        }

        if (isLocked()) {
            if (!mIsProcessing) {
                track(bitmap);
            }
        }

        return bitmap;
    }

    public synchronized void lock(Rectangle rectangle) {
        if (mFrame != null) {
            trackPolygon = rectangle.getRescaled(mFrame.width, mFrame.height).toPolygon();
            trackRectangle = rectangle;
            mInitIsPending = true;
            isLocked = true;
        }
    }

    public synchronized void track(Bitmap bitmap) {
        this.executor.execute(() -> {
            mIsProcessing = true;
            ConvertBitmap.bitmapToBoof(bitmap, mFrame, null);

            if (mInitIsPending) {
                mTracker.initialize(mFrame, trackPolygon);
                mInitIsPending = false;
            } else {
                try {
                    mTracker.process(mFrame, trackPolygon);
                    trackRectangle.update(trackPolygon, mFrame.width, mFrame.height);

                    if (this.mListener != null) {
                        this.executor.execute(() -> mListener.onUpdate(trackRectangle));
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Tracking image size has changed!");
                    mInitIsPending = true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "There was a memory leak / bad references! Method not synchronized?");
                    this.stop();
                }
            }

            mIsProcessing = false;
        });
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void stop() {
        isLocked = false;
        mInitIsPending = false;
    }


}
