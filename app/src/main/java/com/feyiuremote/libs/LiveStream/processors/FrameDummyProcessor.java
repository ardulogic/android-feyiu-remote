package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dummy processor that draws a circle at the center of the overlay.
 * On stop(), the circle turns fully red; on onDeactivate(), it becomes almost invisible.
 */
public class FrameDummyProcessor extends FrameProcessor {
    private int circleColor;
    private int circleAlpha;

    public FrameDummyProcessor(OverlayView overlay) {
        super(overlay);
        // default: white semi-transparent
        circleColor = Color.WHITE;
        circleAlpha = 128;
    }

    @Override
    public void processFrame(CameraFrame frame) {
        circleColor = Color.WHITE;
        circleAlpha = 128;

        OverlayView overlay = overlayView;
        int width = overlay.getWidth();
        int height = overlay.getHeight();
        if (width == 0 || height == 0) return;

        // Calculate circle parameters
        int radius = Math.min(width, height) / 4;
        int cx = width / 2;
        int cy = height / 2;

        // Create a ShapeDrawable circle
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(circleColor);
        circle.getPaint().setAlpha(circleAlpha);
        // Set bounds so shape is drawn centered
        circle.setBounds(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
        );

        // Update overlay with this single drawable
        List<Drawable> list = new ArrayList<>(1);
        list.add(circle);
        overlay.updateOverlay(list);
    }

    @Override
    public boolean providesPOI() {
        return false;
    }


    /**
     * When deactivated, make the circle almost invisible.
     */
    @Override
    public void terminate() {
        circleAlpha = 16; // nearly transparent
    }

    /**
     * When stopped, turn the circle fully red and opaque.
     */
    @Override
    public void stop() {
        Log.d("STOP IS CALLED", "STOP IS CALLED");
        // 1) Update the color/alpha state
        circleColor = Color.RED;
        circleAlpha = 255;

        // 2) Re-draw immediately on the UI thread
        int width = overlayView.getWidth();
        int height = overlayView.getHeight();
        if (width == 0 || height == 0) return;

        int radius = Math.min(width, height) / 4;
        int cx = width / 2;
        int cy = height / 2;

        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(circleColor);
        circle.getPaint().setAlpha(circleAlpha);
        circle.setBounds(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
        );

        // push that single-drawable list straight into the overlay
        overlayView.updateOverlay(Collections.singletonList(circle));
    }

}
