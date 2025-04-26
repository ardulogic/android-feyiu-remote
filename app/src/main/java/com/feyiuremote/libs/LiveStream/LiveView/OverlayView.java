package com.feyiuremote.libs.LiveStream.LiveView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.feyiuremote.libs.LiveStream.LiveView.Listeners.ILiveViewTouchListener;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.OnTouchRectangleDraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * OverlayView.java
 */
public class OverlayView extends View {
    private final List<Drawable> drawables = Collections.synchronizedList(new ArrayList<>());
    public OnTouchRectangleDraw touchProcessor;

    public OverlayView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        attachOnTouchListeners(ctx);
    }

    protected void attachOnTouchListeners(Context context) {
        this.touchProcessor = new OnTouchRectangleDraw(context, this);
        setOnTouchListener(touchProcessor);
    }

    /**
     * Called by processors on any thread
     */
    public void updateOverlay(List<Drawable> newDraws) {
        synchronized (drawables) {
            drawables.clear();
            drawables.addAll(newDraws);
        }
        postInvalidate();   // schedule onDraw() on UI thread
    }

    public void clear() {
        synchronized (drawables) {
            drawables.clear();
        }
        postInvalidate();
    }

    public void setOnTouchListener(ILiveViewTouchListener l) {
        this.touchProcessor.setListener(l);

        setOnTouchListener(this.touchProcessor);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (drawables) {
            for (Drawable d : drawables) {
                d.draw(canvas);
            }
        }
    }
}
