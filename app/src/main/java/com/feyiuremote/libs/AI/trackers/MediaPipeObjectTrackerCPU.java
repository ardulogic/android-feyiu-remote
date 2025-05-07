package com.feyiuremote.libs.AI.trackers;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.Utils.Rectangle;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.ProtoUtil;
import com.google.mediapipe.tracking.BoxTrackerProto;

import java.util.concurrent.locks.ReentrantLock;

/**
 * CPU-based object tracker using MediaPipe’s box_tracking_main_cpu graph.
 */
public class MediaPipeObjectTrackerCPU implements IObjectTracker {
    private static final String TAG = "MediaPipeTracker";

    // 1) Switch to the CPU graph
    private static final String GRAPH_ASSET_NAME = "box_tracking_main_cpu.binarypb";

    // 2) Stream names match your CPU graph snippet:
    //    input_stream: "VIDEO:input_video"
    //    input_stream: "BOXES:start_pos"
    //    input_stream: "CANCEL_ID:cancel_object_id"
    //    output_stream: "BOXES:boxes"
    private static final String VIDEO_STREAM = "input_video";
    private static final String START_POS_STREAM = "start_pos";
    private static final String CANCEL_STREAM = "cancel_object_id";
    private static final String BOXES_STREAM = "boxes";

    private long lastFrameTimestampNs = 0;
    private final FrameProcessor processor;

    private IObjectTrackerListener listener;

    private int frameW, frameH;

    private int boxId = 1;

    private final ReentrantLock frameLock = new ReentrantLock();

    static {
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            System.loadLibrary("opencv_java4");
        }
    }

    public MediaPipeObjectTrackerCPU(Context ctx) {
        AndroidAssetUtil.initializeNativeAssetManager(ctx);

        // Register your proto type as before
        ProtoUtil.registerTypeName(
                BoxTrackerProto.TimedBoxProtoList.class,
                "mediapipe.TimedBoxProtoList"
        );


        // 3) Create a FrameProcessor with no GPU upload / download
        //    (we’ll push ImageFrame packets by hand below)
        processor = new FrameProcessor(
                ctx,
                /*eglContext=*/ 0,
                GRAPH_ASSET_NAME,
                /*inputVideoStreamName=*/ VIDEO_STREAM,
                /*outputVideoStreamName=*/ null
        );

        processor.setVideoInputStreamCpu(VIDEO_STREAM);

        // 4) Listen for tracked boxes on the CPU graph’s output
        processor.addPacketCallback(BOXES_STREAM, this::handleTrackedBoxes);
    }


    private void cancelTracking() {
        Packet cancelPkt =
                processor.getPacketCreator().createInt32(boxId);

        processor.getGraph()
                .addConsumablePacketToInputStream(
                        CANCEL_STREAM,
                        cancelPkt,
                        ++lastFrameTimestampNs);

        boxId++;
    }

    /**
     * Called on every new camera frame. Converts Bitmap → ByteBuffer → ImageFrame
     * and feeds it into the CPU graph.
     */
    public void onNewFrame(CameraFrame frame) {
        // try to grab the lock; if we can't, it means the last frame
        // is still being processed → drop this one immediately
        if (!frameLock.tryLock()) {
            Log.w(TAG, "Skipping frame (processor busy)");
            return;
        }

        try {
            lastFrameTimestampNs = frame.getTimestampNs();
            frameW = frame.bitmap().getWidth();
            frameH = frame.bitmap().getHeight();

            processor.onNewFrame(frame.bitmap(), frame.getTimestampNs());
        } finally {
            frameLock.unlock();
        }
    }

    @Override
    public void lock(Rectangle rect) {
        cancelTracking();

        // 5) Build your TimedBoxProto and push into "start_pos"
        long timeMs = (lastFrameTimestampNs + 999) / 1_000; // ceiling op

        BoxTrackerProto.TimedBoxProto timedBox =
                BoxTrackerProto.TimedBoxProto.newBuilder()
                        .setLeft(rect.getRelativeLeft())
                        .setTop(rect.getRelativeTop())
                        .setRight(rect.getRelativeRight())
                        .setBottom(rect.getRelativeBottom())
                        .setTimeMsec(timeMs)
                        .setId(boxId)
                        .setConfidence(1.0f)
                        .build();

        BoxTrackerProto.TimedBoxProtoList boxList =
                BoxTrackerProto.TimedBoxProtoList.newBuilder()
                        .addBox(timedBox)
                        .build();

        Packet packet = processor.getPacketCreator().createProto(boxList);
        processor.getGraph()
                .addConsumablePacketToInputStream(
                        START_POS_STREAM,
                        packet,
                        lastFrameTimestampNs
                );
    }

    @Override
    public void setListener(IObjectTrackerListener l) {
        this.listener = l;
    }

    @Override
    public void stop() {
        cancelTracking();
    }

    @Override
    public void shutdown() {
        stop();
    }

    private void handleTrackedBoxes(Packet packet) {
        BoxTrackerProto.TimedBoxProtoList list =
                PacketGetter.getProto(
                        packet,
                        BoxTrackerProto.TimedBoxProtoList.parser()
                );
        for (BoxTrackerProto.TimedBoxProto box : list.getBoxList()) {
            float l = box.getLeft(), t = box.getTop();
            float r = box.getRight(), b = box.getBottom();
            int x1 = (int) (l * frameW), y1 = (int) (t * frameH);
            int x2 = (int) (r * frameW), y2 = (int) (b * frameH);


            Rectangle rect = new Rectangle(x1, y1, x2, y2, frameW, frameH);

            listener.onPOIUpdate(new POI(rect, box.getConfidence()));
        }
    }

}
