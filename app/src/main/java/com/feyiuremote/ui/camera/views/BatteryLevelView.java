package com.feyiuremote.ui.camera.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BatteryLevelView extends View {

    private Paint batteryPaint;
    private Paint barPaint;
    private Paint backgroundPaint;

    private int batteryLevel = 100; // 0 - 100

    public BatteryLevelView(Context context) {
        super(context);
        init();
    }

    public BatteryLevelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Battery outline (white)
        batteryPaint = new Paint();
        batteryPaint.setColor(Color.WHITE);
        batteryPaint.setStyle(Paint.Style.STROKE);
        batteryPaint.setStrokeWidth(6f);

        // Battery fill bars
        barPaint = new Paint();
        barPaint.setColor(Color.WHITE);
        barPaint.setStyle(Paint.Style.FILL);

        // Background inside the battery
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.DKGRAY); // use darker gray for contrast
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void setBatteryLevel(int level) {
        batteryLevel = Math.max(0, Math.min(100, level));
        invalidate(); // Triggers redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float capWidth = width * 0.08f;
        float bodyWidth = width - capWidth - 4;
        float bodyHeight = height - 4;

        // Draw battery background inside body
        canvas.drawRect(2, 2, bodyWidth + 2, bodyHeight + 2, backgroundPaint);

        // Draw battery outline (white)
        canvas.drawRect(2, 2, bodyWidth + 2, bodyHeight + 2, batteryPaint); // body
        canvas.drawRect(bodyWidth + 4, height * 0.25f, width, height * 0.75f, batteryPaint); // cap

        // Calculate bars to show
        int barsToShow = 0;
        if (batteryLevel >= 66) barsToShow = 3;
        else if (batteryLevel >= 33) barsToShow = 2;
        else if (batteryLevel > 0) barsToShow = 1;

        // Draw bars
        float spacing = 8f;
        float barWidth = (bodyWidth - 4 * spacing) / 3;
        float barHeight = bodyHeight - 16f;
        float top = 10f;

        for (int i = 0; i < barsToShow; i++) {
            float left = spacing * (i + 1) + barWidth * i + 2;
            canvas.drawRect(left, top, left + barWidth, top + barHeight, barPaint);
        }
    }
}