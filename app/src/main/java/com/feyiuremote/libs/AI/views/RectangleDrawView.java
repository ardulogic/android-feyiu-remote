package com.feyiuremote.libs.AI.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class RectangleDrawView extends View {

    private String TAG = "RectangleDrawView";
    private IRectangleDrawViewListener mListener;

    private boolean mShowRect = false;
    private int MIN_RECT_SIZE = 50;


    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public RectangleDrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.setOnTouchListener(mOnTouchListener);
    }

    public void setOnDrawListener(IRectangleDrawViewListener listener) {
        this.mListener = listener;
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int X = (int) event.getX();
            final int Y = (int) event.getY();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "MotionEvent.ACTION_UP");
                    if (Math.abs(y2 - y1) * Math.abs(x2 - x1) > MIN_RECT_SIZE) {
                        if (mListener != null) {
                            mListener.onNewRectangle(x1, y1, x2, y2);
                            clear();
                        }
                    } else {
                        clear();

                        if (mListener != null) {
                            mListener.onClear();
                        }
                    }

                    postInvalidate();
                    break;
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "MotionEvent.ACTION_DOWN");
                    x1 = X;
                    y1 = Y;
                    x2 = X;
                    y2 = Y;

                    mShowRect = true;
                    postInvalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "MotionEvent.ACTION_MOVE");
                    x2 = X;
                    y2 = Y;

                    mShowRect = true;
                    postInvalidate();
                break;
            }

            return true;
        }
    };

    @Override
    public synchronized void draw(final Canvas canvas) {
        super.draw(canvas);

        if (mShowRect) {
            Paint paint = new Paint();
            paint.setColor(Color.rgb(0, 0, 255));
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.STROKE);

            canvas.drawRect(x1, y1, x2, y2, paint);
        }
    }

    public void clear() {
        mShowRect = false;
    }

}
