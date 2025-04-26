package com.feyiuremote.libs.LiveStream.processors;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.processors.abstracts.IFrameProcessor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * FrameProcessorDispatcher.java
 */
public class FrameProcessorDispatcher {
    private final ExecutorService executor;
    private IFrameProcessor currentProcessor;

    public FrameProcessorDispatcher() {
        executor = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    public synchronized void setFrameProcessor(IFrameProcessor processor) {
        if (currentProcessor != null) {
            currentProcessor.onDeactivate();
        }
        currentProcessor = processor;
    }

    public void processFrame(CameraFrame frame, OverlayView overlayView) {
        IFrameProcessor proc;

        synchronized (this) {
            proc = currentProcessor;
        }
        if (proc != null) {
            executor.submit(() -> proc.processFrame(frame, overlayView));
        }
    }
}
