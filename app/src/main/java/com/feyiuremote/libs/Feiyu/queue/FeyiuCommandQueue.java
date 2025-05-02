package com.feyiuremote.libs.Feiyu.queue;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommand;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandLooselyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandStrictlyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.SingleCommand;
import com.feyiuremote.libs.Feiyu.queue.commands.SingleSensitivityCommand;
import com.feyiuremote.libs.Feiyu.queue.debug.QueueStopDebugger;
import com.feyiuremote.libs.Feiyu.queue.entries.QueueJoyStateLooseEntry;
import com.feyiuremote.libs.Feiyu.queue.entries.QueueEntry;
import com.feyiuremote.libs.Feiyu.queue.entries.QueueJoyStateEntry;
import com.feyiuremote.libs.Feiyu.queue.entries.QueueSensitivityStateEntry;
import com.feyiuremote.libs.Feiyu.queue.entries.QueueJoyStateStrictEntry;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Scheduler for joystick pan/tilt commands – compatible with JDK6/7 (no lambdas, no java.util.Objects).
 */
public final class FeyiuCommandQueue implements Runnable, Handler.Callback {
    private final static String TAG = FeyiuCommandQueue.class.getSimpleName();

    public enum Axis {PAN, TILT}

    public static final long MIN_INTERVAL_MS = 40;
    public static final long DEFAULT_INTERVAL_MS = 100;
    public static final long MAX_INTERVAL_MS = 150;
    private static BluetoothLeService bluetoothService = null;
    private static final int MSG_FIRE = 1;

    private static final HandlerThread TIMER_THREAD =
            new HandlerThread("cmd-dispatcher",
                    android.os.Process.THREAD_PRIORITY_DISPLAY);

    private static final FeyiuCommandQueue CALLBACK = new FeyiuCommandQueue();

    /**
     * All commands are fired from this Handler (never the UI thread).
     */
    private static final Handler TIMER;

    static {               // static initialiser – starts once for the whole app
        TIMER_THREAD.start();
        TIMER = new Handler(TIMER_THREAD.getLooper(), CALLBACK);
    }

    public static void assignBluetoothService(BluetoothLeService btService) {
        bluetoothService = btService;
    }

    public static void shutdown() {
        TIMER.removeCallbacksAndMessages(null);  // stop future fires
        TIMER_THREAD.quitSafely();               // end Looper after queue drains
        scheduledCommands.clear();
    }

    public static void cancelQueuedCommands() {
        scheduledCommands.clear();
        singleCommands.clear();
    }

    private static void rescheduleIfNeeded() {
        if (scheduledCommands.isEmpty() && singleCommands.isEmpty()) {
            TIMER.removeMessages(MSG_FIRE);        // nothing to do
            return;
        }

        Long headTs;

        if (singleCommands.isEmpty()) {
            headTs = scheduledCommands.firstKey();    // earliest exec-time
        } else {
            headTs = singleCommands.firstKey();    // earliest exec-time
        }

        long delay = headTs - System.currentTimeMillis();                    // may be negative

        // The only message we ever keep in the queue:
        TIMER.removeMessages(MSG_FIRE);
        TIMER.sendEmptyMessageDelayed(MSG_FIRE, Math.max(0, delay));
    }

    private static final ConcurrentNavigableMap<Long, QueueEntry> scheduledCommands =
            new ConcurrentSkipListMap<>();

    private static final ConcurrentNavigableMap<Long, QueueEntry> singleCommands =
            new ConcurrentSkipListMap<>();

    public static void submit(JoyCommand cmd) {
        if (cmd instanceof JoyCommandLooselyTimed) {
            seedLooseJoyCommand((JoyCommandLooselyTimed) cmd);
        } else if (cmd instanceof JoyCommandStrictlyTimed) {
            seedStrictJoyCommand((JoyCommandStrictlyTimed) cmd);
        } else {
            throw new IllegalArgumentException("Unsupported JoyCommand: " + cmd);
        }
    }

    public static void submitSingle(SingleCommand cmd) {
        if (cmd instanceof SingleSensitivityCommand) {
            seedSingleCommand(cmd);
        }
    }

    public static long getSoonestExecutionTimestamp() {
        long timeSinceLastCmd = FeyiuState.getInstance().getTimeSinceLastCommandMs();
        return System.currentTimeMillis() + MIN_INTERVAL_MS - Math.min(timeSinceLastCmd, MIN_INTERVAL_MS);
    }

    private static void seedSingleCommand(SingleCommand cmd) {
        final NavigableMap<Long, QueueEntry> q = singleCommands;

        // Build the new queue entry
        QueueSensitivityStateEntry newEntry = new QueueSensitivityStateEntry((SingleSensitivityCommand) cmd);
        if (!newEntry.needsToExecute()) {
            Log.d(TAG, "Skipping sensitivity change.");
            return;
        }

        // Determine next timestamp: 40ms after the last entry in the queue
        long nextTimestamp;
        if (q.isEmpty()) {
            // If queue is empty, start from "now"
            nextTimestamp = getSoonestExecutionTimestamp();
        } else {
            // Peek at the highest key (last entry) and add 40ms
            Map.Entry<Long, QueueEntry> lastEntry = q.lastEntry();
            if (lastEntry == null) {
                nextTimestamp = getSoonestExecutionTimestamp();
            } else {
                nextTimestamp = lastEntry.getKey() + MIN_INTERVAL_MS;
            }
        }

        newEntry.updateTimestamp(nextTimestamp);

        // Insert the new entry and trigger scheduling
        q.put(nextTimestamp, newEntry);

        scheduleSingle(newEntry);
        rescheduleIfNeeded();
    }


    public static void seedLooseJoyCommand(JoyCommandLooselyTimed cmd) {
        final long now = System.currentTimeMillis();
        final long execDelay = Math.max(0, DEFAULT_INTERVAL_MS - FeyiuState.getInstance().getTimeSinceLastCommandMs());
        final long horizonTs = now + cmd.durationMs;

        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        if (q.isEmpty()) {
            for (long off = execDelay; off < cmd.durationMs; off += DEFAULT_INTERVAL_MS) {
                QueueJoyStateLooseEntry newEntry = new QueueJoyStateLooseEntry(cmd, now + off);
                schedule(newEntry);
            }
        } else {
            // Overwrite existing commands and remove ones that are past duration
            removeOrOverwriteExistingCommands(cmd);

            // Add missing commands
            long nextTs = q.isEmpty() ? now + execDelay : q.lastKey() + DEFAULT_INTERVAL_MS;

            for (; nextTs <= horizonTs; nextTs += DEFAULT_INTERVAL_MS) {
                QueueJoyStateLooseEntry newEntry = new QueueJoyStateLooseEntry(cmd, nextTs);
                schedule(newEntry);
            }
        }

        rescheduleIfNeeded();
    }

    private static void addMissingCommands(JoyCommand cmd) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;
        long nextTs = q.isEmpty() ? cmd.startTime : q.lastKey() + DEFAULT_INTERVAL_MS;

        for (; nextTs <= cmd.endTime; nextTs += DEFAULT_INTERVAL_MS) {
            QueueJoyStateLooseEntry newEntry = new QueueJoyStateLooseEntry(cmd, nextTs);
            schedule(newEntry);
        }
    }

    private static void removeCommandsBeforeTimestamp(Long timestamp) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        for (Iterator<Map.Entry<Long, QueueEntry>> it = q.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, QueueEntry> e = it.next();
            QueueEntry entry = e.getValue();

            if (entry.executesAt() < timestamp) {
                it.remove();
            }
        }
    }

    private static void removeOrOverwriteExistingCommands(JoyCommand cmd) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        for (Iterator<Map.Entry<Long, QueueEntry>> it = q.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, QueueEntry> e = it.next();
            QueueEntry entry = e.getValue();

            if (entry instanceof QueueJoyStateEntry) {
                if (entry.executesAt() > cmd.startTime) {
                    ((QueueJoyStateEntry) entry).overwriteWith(cmd);
                }

                if (entry.executesAt() > cmd.endTime) {
                    if (((QueueJoyStateEntry) entry).axisHasNoValue(cmd.getOppositeAxis())) {
                        it.remove();
                    } else {
                        ((QueueJoyStateEntry) entry).overwriteWith(cmd.axis, null);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends QueueEntry> T getFirstPreviousQueueEntryOfType(Long startTime, Class<T> type) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;
        NavigableMap<Long, QueueEntry> headMap = q.headMap(startTime, false);

        for (Map.Entry<Long, QueueEntry> entry : headMap.descendingMap().entrySet()) {
            if (type.isInstance(entry.getValue())) {
                return (T) entry.getValue();
            }
        }

        return null;
    }

    private static void seedStrictJoyCommand(JoyCommandStrictlyTimed cmd) {

        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        QueueJoyStateEntry previousJoyEntry = getFirstPreviousQueueEntryOfType(cmd.startTime, QueueJoyStateEntry.class);
        QueueJoyStateStrictEntry newEntry = new QueueJoyStateStrictEntry(cmd);

        if (newEntry.executesIn() < 0) {
            throw new RuntimeException("Cannot have command executing earlier than now: " + newEntry.executesIn() + "ms");
        }

        if (previousJoyEntry != null) {
            newEntry.copyInitialValuesFrom(previousJoyEntry);
        }

        // Check if we should merge with previous strict state
        Map.Entry<Long, QueueEntry> before = q.lowerEntry(cmd.startTime);
        boolean shouldMergeWithPrevious = shouldMergeWithNeighbour(cmd, before);

        // Check if we should merge with next strict state
        Map.Entry<Long, QueueEntry> after = q.higherEntry(cmd.startTime);
        boolean shouldMergeWithNext = shouldMergeWithNeighbour(cmd, after);

        if (shouldMergeWithPrevious || shouldMergeWithNext) {
            mergeWithNeighbour(cmd, shouldMergeWithPrevious ? before : after);
            removeOrOverwriteExistingCommands(cmd);
            addMissingCommands(cmd);
            rescheduleIfNeeded();
            return;
        }

        // 3. shift mutable neighbours so we keep a safety gap
        if (isTooCloseInTime(before, newEntry)) {
            moveInTime(before, newEntry.executesAt() - MIN_INTERVAL_MS);
        }

        if (isTooCloseInTime(after, newEntry)) {
            moveInTime(after, newEntry.executesAt() + MIN_INTERVAL_MS);
        }

        // 4. schedule the new command itself
        removeOrOverwriteExistingCommands(cmd);

        q.put(cmd.startTime, newEntry);
        addMissingCommands(cmd);

        rescheduleIfNeeded();
    }

    private static boolean isTooCloseInTime(Map.Entry<Long, QueueEntry> neighbour, QueueEntry target) {
        if (neighbour != null && target != null) {
            return Math.abs((neighbour.getValue().executesAt() - target.executesAt())) < MIN_INTERVAL_MS;
        }

        return false;
    }

    /**
     * Returns {@code true} when the neighbour is immutable **and** the gap to it is
     * smaller than {@link #MIN_INTERVAL_MS}.
     */
    private static boolean shouldMergeWithNeighbour(JoyCommandStrictlyTimed cmd,
                                                    Map.Entry<Long, QueueEntry> neighbour) {

        if (neighbour != null) {
            QueueEntry entry = neighbour.getValue();

            if (entry instanceof QueueJoyStateStrictEntry) {
                long gap = Math.abs(cmd.startTime - neighbour.getKey());

                return gap < MIN_INTERVAL_MS;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} when the neighbour is immutable **and** the gap to it is
     * smaller than {@link #MIN_INTERVAL_MS}.
     */
    private static boolean shouldCancelNeighbour(SingleSensitivityCommand cmd,
                                                 Map.Entry<Long, QueueEntry> neighbour) {

        if (neighbour != null) {
            QueueEntry entry = neighbour.getValue();

            if (entry instanceof QueueJoyStateLooseEntry) {
                long gap = Math.abs(cmd.startTime - neighbour.getKey());

                return gap < MIN_INTERVAL_MS;
            }
        }

        return false;
    }

    /**
     * Performs the actual merge.
     * Preconditions (checked by {@code shouldMergeWithNeighbour}): neighbour is immutable,
     * gap &lt; MIN_INTERVAL_MS.
     */
    private static void mergeWithNeighbour(JoyCommandStrictlyTimed cmd,
                                           Map.Entry<Long, QueueEntry> neighbour) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        // Remove neighbour
        QueueJoyStateEntry neighbourQueueEntry = (QueueJoyStateEntry) neighbour.getValue();
        q.remove(neighbourQueueEntry.executesAt());

        // Insert merged entry
        QueueJoyStateStrictEntry newEntry = new QueueJoyStateStrictEntry(cmd);
        neighbourQueueEntry.mergeWith(newEntry);
        q.put(neighbourQueueEntry.executesAt(), neighbourQueueEntry);

        cmd.updateStartTime(neighbourQueueEntry.executesAt());
    }

    /**
     * If the neighbour is mutable and infringes the protected interval, move it just
     * outside the limit.
     */
    private static void moveInTime(Map.Entry<Long, QueueEntry> mapEntry, long newTimestamp) {
        final NavigableMap<Long, QueueEntry> q = scheduledCommands;

        if (mapEntry != null) {
            QueueEntry entry = mapEntry.getValue();
            entry.updateTimestamp(newTimestamp);

            q.remove(mapEntry.getKey());
            q.put(newTimestamp, entry);
        }
    }

    public static void schedule(QueueEntry entry) {
        scheduledCommands.put(entry.executesAt(), entry);
    }

    public static void scheduleSingle(QueueEntry entry) {
        singleCommands.put(entry.executesAt(), entry);
    }


    @Override
    public void run() {
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what != MSG_FIRE) return false;

        QueueEntry qe;
        Map.Entry<Long, QueueEntry> e = singleCommands.pollFirstEntry();
        if (e != null) {
            qe = e.getValue();
        } else {
            Map.Entry<Long, QueueEntry> scheduled = scheduledCommands.pollFirstEntry();
            qe = scheduled.getValue();

            if (qe == null) {
                return true;
            }
        }

        // 3) Dispatch and reschedule as before
        try {
            sendCommand(qe);
        } catch (Exception ex) {
            Log.w(TAG, "Command dispatch failed", ex);
        }

        // Immediately post the next wake-up if there’s more to run
        rescheduleIfNeeded();
        return true;
    }


    public void sendCommand(QueueEntry qe) {
        FeyiuState.getInstance().last_command = System.currentTimeMillis();

        Long timingError = qe.executesIn();

        if (qe instanceof QueueJoyStateStrictEntry) {
            QueueJoyStateStrictEntry qes = (QueueJoyStateStrictEntry) qe;
            Integer panVal = qes.panJoyValue;
            Integer tiltVal = qes.tiltJoyValue;

            if (panVal != null && panVal == 0) {
                QueueStopDebugger.onPanStopCommand();
            } else if (tiltVal != null && tiltVal == 0) {
                QueueStopDebugger.onTiltStopCommand();
            }
            Log.w(TAG, "Sending (" + qe.executesIn() + "ms): " + qe.getCommand(null).toString());
        } else {

            if (timingError > 10) {
                Log.w(TAG, "Sending (" + qe.executesIn() + "ms): " + qe.getCommand(null).toString());
            } else {
                Log.d(TAG, "Sending (" + qe.executesIn() + "ms): " + qe.getCommand(null).toString());
            }
        }

        //TODO: Uncomment this
        qe.getCommand(bluetoothService).run();
    }

    public static String asString() {
        long current_ts = System.currentTimeMillis();
        long last_ts = 0;

        StringBuilder sb = new StringBuilder("ScheduledCommands:\n");
        for (Map.Entry<Long, QueueEntry> entry : scheduledCommands.entrySet()) {
            long timestamp = entry.getKey();
            QueueEntry queueEntry = entry.getValue();

            sb
                    .append(queueEntry instanceof QueueJoyStateStrictEntry ? "i" : "")
                    .append(queueEntry instanceof QueueJoyStateStrictEntry && ((QueueJoyStateStrictEntry) queueEntry).isMerged ? "m" : "")
                    .append("  • [")
                    .append(timestamp - current_ts).append(" ↑").append(last_ts > 0 ? timestamp - last_ts : 0)
//                    .append(" ↑").append(last_ts > 0 ? entry.getValue().executesAt() - last_ts : 0)
                    .append("] → ")
                    .append(queueEntry.getCommand(null).toString()).append("\n");

            last_ts = timestamp;
        }

        return sb.toString();
    }


}
