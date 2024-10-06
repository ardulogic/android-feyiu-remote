package com.feyiuremote.libs.LiveStream.LiveView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.feyiuremote.libs.LiveStream.LiveView.Listeners.ILiveViewTouchListener;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.OnTouchRectangleDraw;

public class LiveImageView extends TextureView implements TextureView.SurfaceTextureListener {

    protected Bitmap bitmap;
    protected Context context;
    public OnTouchRectangleDraw touchProcessor;

    public LiveImageView(Context context) {
        super(context);
        init(context);
    }

    public LiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        this.context = context;
        this.touchProcessor = new OnTouchRectangleDraw(this.context, this);

        setSurfaceTextureListener(this);
    }

    public void setOnTouchListener(ILiveViewTouchListener l) {
        this.touchProcessor.setListener(l);

        setOnTouchListener(this.touchProcessor);
    }

    public Bitmap getFrame() {
        return this.bitmap;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // Handle the available surface texture
        drawFrame();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Handle changes to the surface texture
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Clean up resources here if needed
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Handle the texture update
    }

    public void setFrameBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        drawFrame();
    }

    protected void drawContent(Canvas canvas) {
        if (this.bitmap != null) {
            drawBitmap(canvas);
        }
    }

    private void drawFrame() {
        Canvas canvas = lockCanvas(); // Lock the canvas for drawing
        if (canvas != null) {
            drawContent(canvas);
            unlockCanvasAndPost(canvas); // Unlock the canvas and post the changes
        }
    }

    private void drawBitmap(Canvas canvas) {
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
        // Draw the bitmap on the canvas
        canvas.drawBitmap(bitmap, null, new RectF(0, 0, getWidth(), getHeight()), null);
    }
}
