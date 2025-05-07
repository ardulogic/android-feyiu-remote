package com.feyiuremote.libs.LiveStream.LiveView.Listeners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnTouchRectangleDraw implements View.OnTouchListener {
    private static final String TAG = "RectangleDrawTouchListener";
    private static final int MIN_RECT_SIZE = 30;
    private static final int TAP_MOVE_THRESHOLD = 5; // px

    private int drawAreaW;
    private int drawAreaH;

    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private boolean moved;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> singleTapFuture;

    private GestureDetector gestureDetector;
    private ILiveViewTouchListener mListener;


    public OnTouchRectangleDraw(Context context) {
        initGestureDetector(context);
    }

    private void initGestureDetector(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                if (mListener != null) {
                    mListener.onLongPress();
                }
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (mListener != null) {
                    // Cancel the pending single-tap execution
                    if (singleTapFuture != null && !singleTapFuture.isDone()) {
                        singleTapFuture.cancel(true);
                        Log.d(TAG, "Cancelled pending single tap");
                    }

                    mListener.onDoubleTap((int) e.getX(), (int) e.getY(), drawAreaW, drawAreaH);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                if (mListener != null) {
                    mListener.onSingleTap((int) e.getX(), (int) e.getY(), drawAreaW, drawAreaH);
                }

                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
//                if (mListener != null) {
//                    mListener.onSingleTap((int) e.getX(), (int) e.getY(), drawAreaW, drawAreaH);
//                }

                return super.onSingleTapConfirmed(e);
            }
        });
    }

    public void setListener(ILiveViewTouchListener listener) {
        this.mListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "RectangleDrawWorks");
        gestureDetector.onTouchEvent(event);

        drawAreaW = v.getWidth();
        drawAreaH = v.getHeight();

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
                notifyFail();
                break;
        }

        return true;
    }

    private void handleActionDown(int x, int y) {
        resetState(x, y);
        notifyDrawing(startX, startY, endX, endY);
    }

    private void handleActionMove(int x, int y) {
        updateDrag(x, y);
        notifyDrawing(startX, startY, endX, endY);
    }

    private void handleActionUp(int x, int y) {
        updateDrag(x, y);

        if (moved) {
            if (isValidDrag()) {
                notifyNewRectangle(startX, startY, endX, endY);
            } else {
                notifyFail();
            }
        }
    }

    private void resetState(int x, int y) {
        moved = false;
        startX = x;
        startY = y;
        endX = x;
        endY = y;
    }

    private void updateDrag(int x, int y) {
        if (!moved && (Math.abs(x - startX) > TAP_MOVE_THRESHOLD || Math.abs(y - startY) > TAP_MOVE_THRESHOLD)) {
            moved = true;
        }
        endX = x;
        endY = y;
    }

    private boolean isValidDrag() {
        return Math.abs(endX - startX) > MIN_RECT_SIZE && Math.abs(endY - startY) > MIN_RECT_SIZE;
    }

    private void notifySingleTap(int x, int y) {
        if (mListener != null) {

            // Cancel any previously scheduled single tap
            if (singleTapFuture != null && !singleTapFuture.isDone()) {
                singleTapFuture.cancel(false);
            }

            singleTapFuture = executor.schedule(() -> {
                mListener.onSingleTap(x, y, drawAreaW, drawAreaH);
            }, 250, TimeUnit.MILLISECONDS); // 250ms window to detect double tap
        }
    }

    private void notifyDrawing(int x1, int y1, int x2, int y2) {
        if (mListener != null) {
            mListener.onDrawingRectangle(x1, y1, x2, y2, drawAreaW, drawAreaH);
        }
    }

    private void notifyNewRectangle(int x1, int y1, int x2, int y2) {
        if (mListener != null) {
            mListener.onNewRectangle(x1, y1, x2, y2, drawAreaW, drawAreaH);
        }
    }

    private void notifyFail() {
        if (mListener != null) {
            mListener.onDrawingRectangleFail();
        }
    }
}
