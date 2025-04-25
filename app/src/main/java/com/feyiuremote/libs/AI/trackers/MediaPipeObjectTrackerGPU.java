package com.feyiuremote.libs.AI.trackers;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.feyiuremote.libs.AI.trackers.mediapipe.GpuTexture;
import com.feyiuremote.libs.Utils.Rectangle;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.ProtoUtil;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.glutil.EglManager;
import com.google.mediapipe.tracking.BoxTrackerProto;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple object-tracker that wraps the MediaPipe graph
 * defined in object_tracking_only_gpu.binarypb.
 */
public class MediaPipeObjectTrackerGPU implements IObjectTracker {

    /* ---------- constants that match your graph ---------- */
    private static final String GRAPH_ASSET_NAME = "box_tracking_main_gpu.binarypb";
    private static final String VIDEO_STREAM = "input_video";
    private static final String DETECTION_STREAM = "input_detections";
    private static final String TRACKED_STREAM = "tracked_detections";
    private static final String TAG = "MediaPipeTracker";
    /* ----------------------------------------------------- */

    private long lastFrameTimestampUs = 0;
    private final EglManager eglManager;
    private final FrameProcessor processor;

    private IObjectTrackerListener listener;
    private final AtomicReference<Rectangle> lastRect = new AtomicReference<>();

    private int frameW, frameH;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            // Some example apps (e.g. template matching) require OpenCV 4.
            System.loadLibrary("opencv_java4");
        }
    }

    private boolean videoHeaderSent = false;

    public MediaPipeObjectTrackerGPU(ExecutorService executor, Context ctx) {
        AndroidAssetUtil.initializeNativeAssetManager(ctx);

        ProtoUtil.registerTypeName(
                BoxTrackerProto.TimedBoxProtoList.class,
                "mediapipe.TimedBoxProtoList"
        );

        eglManager = new EglManager(null);
        processor = new FrameProcessor(
                ctx,
                eglManager.getNativeContext(),
                GRAPH_ASSET_NAME,
                VIDEO_STREAM,          // graph expects a video stream but does not output one
                null);


        /* callback for every TimedBoxProtoList packet -------------------- */
        processor.addPacketCallback(TRACKED_STREAM, this::handleTrackedBoxes);
    }

    /* ---------------- IObjectTracker implementation ------------------- */

    private Bitmap generateWhiteNoise(int width, int height) {
        Bitmap noise = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        Random rng = new Random();
        for (int i = 0; i < pixels.length; i++) {
            // generate a gray value [0..255], you can also do full-color
            int g = rng.nextInt(256);
            // pack into opaque ARGB
            pixels[i] = 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        noise.setPixels(pixels, 0, width, 0, 0, width, height);
        return noise;
    }

    public Bitmap onNewFrame(Bitmap bmp) {
        if (lastFrameTimestampUs >= SystemClock.elapsedRealtimeNanos()) {
            // Sometimes this can happen (a buffered frame from the past)
            return bmp;
        }

        if (!videoHeaderSent) {
            AndroidPacketCreator packetCreator = processor.getPacketCreator();
            Packet videoHeader =
                    packetCreator.createVideoHeader(bmp.getWidth(), bmp.getHeight());

            processor.getGraph()
                    .setStreamHeader("input_video", videoHeader);

//            videoHeaderSent = true;

//            return bmp;
        }

        // 1. Remember the size (for your own rect conversions, etc.)
        frameW = bmp.getWidth();
        frameH = bmp.getHeight();

        // 2. Build a TextureFrame from that Bitmap (uploading it into GL)
        lastFrameTimestampUs = SystemClock.elapsedRealtimeNanos() / 1_000;
        TextureFrame gpuFrame = GpuTexture.bitmapToGpuTexture(bmp, lastFrameTimestampUs);

        // 3. Feed it straight into the GPU‐only graph
        //    FrameProcessor.onNewFrame(TextureFrame) will take ownership
        processor.onNewFrame(gpuFrame);

        // 4. Return the original Bitmap if you need to display it unchanged
        return bmp;
    }

    @Override
    public void lock(Rectangle rect) {
        long timeMsec = lastFrameTimestampUs / 1_000;  // TimedBoxProto.timeMsec is in ms

        BoxTrackerProto.TimedBoxProto timedBox =
                BoxTrackerProto.TimedBoxProto.newBuilder()
                        .setLeft(rect.getRelativeLeft())
                        .setTop(rect.getRelativeTop())
                        .setRight(rect.getRelativeRight())
                        .setBottom(rect.getRelativeBottom())
                        .setTimeMsec(timeMsec)
                        .setId(1)
                        .setConfidence(1.0f)
                        .build();

        BoxTrackerProto.TimedBoxProtoList boxList =
                BoxTrackerProto.TimedBoxProtoList.newBuilder()
                        .addBox(timedBox)
                        .build();


        // 4) Convert to a Packet and push into “input_detections” with that same timestamp:
        Packet packet = processor.getPacketCreator().createProto(boxList);
        processor.getGraph()
                .addConsumablePacketToInputStream(
                        DETECTION_STREAM,   // "input_detections"
                        packet,
                        lastFrameTimestampUs);  // µs, same clock as your frames
    }

    @Override
    public void setListener(IObjectTrackerListener l) {
        this.listener = l;
    }

    @Override
    public void stop() {
        processor.close();
        eglManager.release();
    }

    /* --------------------- private helpers ---------------------------- */

    private void handleTrackedBoxes(Packet packet) {
        try {
            // 1) Unpack the packet into our TimedBoxProtoList
            BoxTrackerProto.TimedBoxProtoList list =
                    PacketGetter.getProto(
                            packet,
                            BoxTrackerProto.TimedBoxProtoList.parser()
                    );

            // 2) For each tracked box, pull out its corners, time, and ID
            for (BoxTrackerProto.TimedBoxProto box : list.getBoxList()) {
                float left = box.getLeft();      // normalized [0..1]
                float top = box.getTop();
                float right = box.getRight();
                float bottom = box.getBottom();
                long timeMs = box.getTimeMsec();  // in ms
                int id = box.hasId() ? box.getId() : -1;
                float confidence = box.getConfidence();

                // Convert normalized coords back to pixel-space
                int x1 = (int) (left * frameW);
                int y1 = (int) (top * frameH);
                int x2 = (int) (right * frameW);
                int y2 = (int) (bottom * frameH);

                // Log both normalized and pixel coords
                Log.i(TAG, String.format(
                        "TrackedBox(id=%d @ %dms): [L=%.3f, T=%.3f, R=%.3f, B=%.3f]  px:[%d,%d,%d,%d, confidence: %f]",
                        id, timeMs, left, top, right, bottom, x1, y1, x2, y2, confidence
                ));

                // Notify listener with the pixel-space Rectangle
                Rectangle trackingRect = new Rectangle(x1, y1, x2, y2, frameW, frameH);
                listener.onUpdate(trackingRect);
            }
        } finally {
            // 4) Always release the packet when you're done
        }
    }

}
