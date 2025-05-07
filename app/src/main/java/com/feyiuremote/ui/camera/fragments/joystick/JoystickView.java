package com.feyiuremote.ui.camera.fragments.joystick;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.feyiuremote.R;

public class JoystickView extends View {

    private Bitmap bgBitmap;    // scaled background ring
    private Bitmap knobBitmap;  // scaled knob

    private Bitmap rawBg;       // original size, used for rescaling
    private Bitmap rawKnob;

    private float centerX, centerY;
    private float baseRadius;
    private float knobRadius;

    private final PointF knobPosition = new PointF();
    private @Nullable JoystickListener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float deadzone = 0;

    // ---------------------------------------------------------------------------------------------
    //  Constructors
    // ---------------------------------------------------------------------------------------------
    public JoystickView(Context ctx) {
        super(ctx);
        init(ctx);
    }

    public JoystickView(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx);
    }

    public JoystickView(Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init(ctx);
    }

    // ---------------------------------------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------------------------------------
    public void setJoystickListener(@Nullable JoystickListener listener) {
        this.listener = listener;
    }

    /**
     * Set deadzone threshold (0–100); input inside this range will be treated as 0
     */
    public void setDeadzone(int dz) {
        this.deadzone = Math.max(0, Math.min(1, (float) dz / 100)); // clamp between 0 and 1
    }

    /**
     * Replace background vector PNG/XML at run-time
     */
    public void setBackgroundImage(@DrawableRes int resId) {
        rawBg = drawableToBitmap(ContextCompat.getDrawable(getContext(), resId));
        requestLayout();
    }

    /**
     * Replace knob vector PNG/XML at run-time
     */
    public void setKnobImage(@DrawableRes int resId) {
        rawKnob = drawableToBitmap(ContextCompat.getDrawable(getContext(), resId));
        requestLayout();
    }

    // ---------------------------------------------------------------------------------------------
    //  Initial setup
    // ---------------------------------------------------------------------------------------------
    private void init(Context ctx) {
        rawBg = drawableToBitmap(ContextCompat.getDrawable(ctx, R.drawable.joystick_ring));
        rawKnob = drawableToBitmap(ContextCompat.getDrawable(ctx, R.drawable.joystick_knob));
    }

    // ---------------------------------------------------------------------------------------------
    //  Measurement & scaling
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Keep the view square by enforcing the smaller of width/height
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        centerX = w / 2f;
        centerY = h / 2f;

        baseRadius = Math.min(w, h) / 2.2f;
        knobRadius = baseRadius / 2.5f;
        knobPosition.set(centerX, centerY);

        // scale cached bitmaps to current size
        bgBitmap = Bitmap.createScaledBitmap(rawBg, w, h, true);
        knobBitmap = Bitmap.createScaledBitmap(rawKnob, (int) (knobRadius * 2), (int) (knobRadius * 2), true);
    }

    // ---------------------------------------------------------------------------------------------
    //  Drawing
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0, 0, paint);
        }

        if (knobBitmap != null) {
            float knobX = knobPosition.x - knobBitmap.getWidth() / 2f;
            float knobY = knobPosition.y - knobBitmap.getHeight() / 2f;
            canvas.drawBitmap(knobBitmap, knobX, knobY, paint);
        }
    }

    // ---------------------------------------------------------------------------------------------
    //  Touch handling
    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        float distance = (float) Math.hypot(dx, dy);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (distance < baseRadius) {
                    knobPosition.set(event.getX(), event.getY());
                } else {
                    float ratio = baseRadius / distance;
                    knobPosition.set(centerX + dx * ratio, centerY + dy * ratio);
                }
                notifyListener();
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                knobPosition.set(centerX, centerY);
                notifyListener();   // send 0,0
                invalidate();
                break;
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------------------------------------
    private void notifyListener() {
        if (listener != null) {
            float rawX = (knobPosition.x - centerX) / baseRadius;
            float rawY = (knobPosition.y - centerY) / baseRadius;

            float percentX = applyDeadzoneAndRescale(rawX);
            float percentY = applyDeadzoneAndRescale(rawY);

            listener.onMove(percentX, percentY);
        }
    }

    private float applyDeadzoneAndRescale(float input) {
        float abs = Math.abs(input);

        if (abs < deadzone) {
            return 0f;
        }

        // Remap from [deadzone, 1] to [0, 1]
        float scaled = (abs - deadzone) / (1f - deadzone);
        return input < 0 ? -scaled : scaled;
    }

    /**
     * Converts any Drawable (incl. Vector) to a Bitmap
     */
    private static Bitmap drawableToBitmap(@Nullable Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable cannot be null");
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // ---------------------------------------------------------------------------------------------
    //  Listener
    // ---------------------------------------------------------------------------------------------
    public interface JoystickListener {
        /**
         * @param xPercent Horizontal displacement (-1 … 1)
         * @param yPercent Vertical displacement (-1 … 1) – up is negative, down is positive
         */
        void onMove(float xPercent, float yPercent);
    }
}
