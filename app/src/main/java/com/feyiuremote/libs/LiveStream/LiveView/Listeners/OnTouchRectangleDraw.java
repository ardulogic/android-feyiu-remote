package com.feyiuremote.libs.LiveStream.LiveView.Listeners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;


public class OnTouchRectangleDraw implements View.OnTouchListener {
    private final String TAG = "RectangleDrawTouchListener";
    private final int MIN_RECT_SIZE = 30;
    private final int drawAreaW;
    private final int drawAreaH;

    private int rect_x1;
    private int rect_y1;
    private int rect_x2;
    private int rect_y2;

    private final GestureDetector gestureDetector;
    private IRectangleDrawTouchListener mListener;

    public OnTouchRectangleDraw(Context context, View liveView) {
        this.drawAreaW = liveView.getWidth();
        this.drawAreaH = liveView.getHeight();
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);

                mListener.onLongPress();
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                mListener.onDoubleTap();

                return true;
            }
        });
    }

    public RectF getRect(int maxWidth, int maxHeight) {
        float ratioW = (float) maxWidth / drawAreaW;
        float ratioH = (float) maxHeight / drawAreaH;

        // Draw the rectangle on the canvas
        return new RectF(rect_x1 * ratioW, rect_y1 * ratioH, rect_x2 * ratioW, rect_y2 * ratioH);
    }

    public void setListener(IRectangleDrawTouchListener listener) {
        this.mListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int X = (int) event.getX();
        final int Y = (int) event.getY();
        gestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "MotionEvent.ACTION_UP");
                if (Math.abs(rect_y2 - rect_y1) > MIN_RECT_SIZE && Math.abs(rect_x2 - rect_x1) > MIN_RECT_SIZE) {
                    if (mListener != null) {
                        mListener.onNewRectangle(rect_x1, rect_y1, rect_x2, rect_y2, drawAreaW, drawAreaH);
                    }
                }

                this.mListener.onTooSmallRectangle();
                break;
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "MotionEvent.ACTION_DOWN");
                rect_x1 = X;
                rect_y1 = Y;
                rect_x2 = X;
                rect_y2 = Y;

                this.mListener.onDrawingRectangle(rect_x1, rect_y1, rect_x2, rect_y2);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "MotionEvent.ACTION_MOVE");
                rect_x2 = X;
                rect_y2 = Y;

                this.mListener.onDrawingRectangle(rect_x1, rect_y1, rect_x2, rect_y2);
                break;
        }

        return true;
    }


}