package com.feyiuremote.libs.LiveStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.LiveStream.image.RawImage;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;

import androidx.annotation.Nullable;

public class LiveImageView extends RectangleDrawView {

    private static final String TAG = LiveImageView.class.getSimpleName();
    private ILiveFeedReceiver mFeedReceiver;
    Bitmap imageBitmap;

    Long bitmapTimestamp = null;

    private long frameRenderStart = 0;

    private Paint textPaint;
    private RectF bitmapRect;


    public LiveImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    private void drawRawImage(Bitmap image) {
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            canvas.drawBitmap(image, null, getBitmapRect(), null);
            canvas = drawRect(canvas);
            unlockCanvasAndPost(canvas);
        }

    }

    private RectF getBitmapRect() {
        if (bitmapRect == null) {
            bitmapRect = new RectF(0, 0, getWidth(), getHeight());
        }
        return bitmapRect;
    }

    public void drawFrame(Bitmap bitmap) {
        try {
            drawRawImage(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw FPS");
        }
    }
}
