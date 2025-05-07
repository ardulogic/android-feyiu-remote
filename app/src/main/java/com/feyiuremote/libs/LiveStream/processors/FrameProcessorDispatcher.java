package com.feyiuremote.libs.LiveStream.processors;

import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * FrameProcessorDispatcher.java
 */
public class FrameProcessorDispatcher {

    public final static String TAG = FrameProcessorDispatcher.class.getSimpleName();
    private ExecutorService executor;
    private FrameProcessor currentProcessor;

    public FrameProcessorDispatcher() {
        executor = makeNewExecutor();
    }

    private ThreadPoolExecutor makeNewExecutor() {
        return new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    public synchronized void setFrameProcessor(FrameProcessor processor) {
        // if the old executor is dead, spin up a new one
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = makeNewExecutor();
        }
        if (currentProcessor != null) {
            currentProcessor.terminate();
        }

        currentProcessor = processor;
    }

    public void processFrame(CameraFrame frame) {
        FrameProcessor proc;

        synchronized (this) {
            proc = currentProcessor;
        }
        if (proc != null) {
            executor.submit(() -> proc.processFrame(frame));
        }
    }

    public void stopProcessor() {
        FrameProcessor proc;

        synchronized (this) {
            proc = currentProcessor;
        }
        if (proc != null) {
            Log.d(TAG, "Stopping processor");
            executor.submit(proc::stop);
        }
    }


    /**
     * Stop the frame processor together with whole thread of dispatcher
     */
    public void terminate() {
        FrameProcessor proc;
        synchronized (this) {
            proc = currentProcessor;
            currentProcessor = null;   // clear out the reference so no new work is accepted
        }
        if (proc != null) {
            // 1) submit the termination call and keep the Future
            Future<?> terminationFuture = executor.submit(proc::terminate);

            // 2) stop accepting any more tasks
            executor.shutdown();

            try {
                // 3a) wait for the terminate() call to finish (up to 1s)
                terminationFuture.get(1, TimeUnit.SECONDS);

                // 3b) wait for the executor thread itself to die (another 1s)
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    // 4) if it’s still alive, force it down
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // could be TimeoutException or ExecutionException
                executor.shutdownNow();
            }
        } else {
            // If there was no processor, just kill the executor right away
            executor.shutdownNow();
        }
    }

    public FrameProcessor getCurrentProcessor() {
        return currentProcessor;
    }

    public void terminateProcessor() {
        this.currentProcessor.terminate();
    }
}
