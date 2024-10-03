package com.feyiuremote.libs.LiveStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.feyiuremote.R;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;

import java.util.Objects;
import java.util.Random;

import androidx.annotation.NonNull;

public class OptimisedLiveImageView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final int DRAW_RECTANGLE = 1;
    private final Random random = new Random();
    private final Handler handler;

    public OptimisedLiveImageView(Context context) {
        super(context);
        setSurfaceTextureListener(this);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == DRAW_RECTANGLE) {
                    drawRandomRectangle();
                    sendEmptyMessageDelayed(DRAW_RECTANGLE, 1000); // Draw every second
                }
            }
        };
    }

    public OptimisedLiveImageView(Context context, AttributeSet args) {
        this(context);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        handler.sendEmptyMessage(DRAW_RECTANGLE);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Handle surface size changes if needed
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        handler.removeMessages(DRAW_RECTANGLE);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Update the rectangles if needed
    }

    private void drawRandomRectangle() {
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture == null) return;

        Surface surface = new Surface(surfaceTexture);
        Canvas canvas = surface.lockCanvas(null);

        // Draw a random-sized rectangle on the canvas
        Paint paint = new Paint();
        paint.setColor(Color.BLUE); // Set the color of the rectangle

        int rectangleWidth = random.nextInt(200) + 50; // Random width between 50 and 250 pixels
        int rectangleHeight = random.nextInt(200) + 50; // Random height between 50 and 250 pixels

        int left = random.nextInt(canvas.getWidth() - rectangleWidth);
        int top = random.nextInt(canvas.getHeight() - rectangleHeight);
        int right = left + rectangleWidth;
        int bottom = top + rectangleHeight;

        // Draw the rectangle on the canvas
        canvas.drawRect(left, top, right, bottom, paint);

        // Unlock and post the canvas to the surface
        surface.unlockCanvasAndPost(canvas);
    }
}