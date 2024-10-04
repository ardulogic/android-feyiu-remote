package com.feyiuremote.libs.LiveStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;

import com.feyiuremote.libs.AI.views.RectangleDrawView;

public class LiveImageView extends RectangleDrawView {

    private static final String TAG = LiveImageView.class.getSimpleName();

    private RectF bitmapRect;
    private Bitmap mBitmap;  // Bitmap to be drawn as background


    public LiveImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // Method to be called to set a new frame and redraw the background
    public void setFrameBitmap(Bitmap bitmap) {
        try {
            mBitmap = bitmap;
            bitmapRect = new RectF(0, 0, getWidth(), getHeight());
            invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw frame", e);
        }
    }

    // Override the onDraw method to draw the bitmap and rectangle
    @Override
    protected void onDraw(Canvas canvas) {
        // First, draw the bitmap as the background if available
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, null, bitmapRect, null);  // Draw the bitmap
        }

        // Call the super class to draw the rectangle on top of the bitmap
        super.onDraw(canvas);
    }

}
