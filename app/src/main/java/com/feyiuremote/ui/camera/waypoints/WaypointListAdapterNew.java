package com.feyiuremote.ui.camera.waypoints;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.feyiuremote.R;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.fragments.waypoints.CameraWaypointsViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A more efficient implementation based on {@link ListAdapter} / {@link androidx.recyclerview.widget.AsyncListDiffer}.
 * Heavy list‑diffing, JSON (de)serialisation and SharedPreferences I/O are now executed on a single
 * background thread so the UI thread never stalls.
 */
public class WaypointListAdapterNew extends ListAdapter<Waypoint, WaypointListAdapterNew.ItemViewHolder>
        implements ItemTouchHelperAdapter {

    private static final String TAG = WaypointListAdapterNew.class.getSimpleName();
    private static final String PREF_KEY_WAYPOINT_LIST = "saved_waypoints";

    private final Context context;
    private final GimbalWaypointsProcessor waypointsProcessor;
    private final MutableLiveData<ArrayList<Waypoint>> liveWaypoints;
    private final MutableLiveData<Boolean> waypointsLoaded;
    private final LifecycleOwner lifecycleOwner;

    /**
     * Single IO thread reused for *all* background work instead of spawning new ones every call
     */
    private static final ScheduledExecutorService ioExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autosaveFuture;
    private final Handler main = new Handler(Looper.getMainLooper());

    public WaypointListAdapterNew(@NonNull Context context,
                                  @NonNull LifecycleOwner lifecycleOwner,
                                  @NonNull CameraWaypointsViewModel viewModel) {
        super(DIFF_CB);
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.liveWaypoints = viewModel.waypointList;
        this.waypointsLoaded = viewModel.waypointsLoaded;
        this.waypointsProcessor = Objects.requireNonNull(viewModel.processor.getValue());

        // Observe VM once; submitList() pushes work to AsyncListDiffer (off‑main)
        liveWaypoints.observe(lifecycleOwner, list -> {
            submitList(list == null ? new ArrayList<>() : new ArrayList<>(list));
            scheduleAutosave();
        });

        if (Boolean.FALSE.equals(waypointsLoaded.getValue())) {
            loadWaypointsAsync();
        }
    }

    //────────────────────────────────────────────────── DiffUtil (off‑main thanks to AsyncListDiffer)
    private static final DiffUtil.ItemCallback<Waypoint> DIFF_CB = new DiffUtil.ItemCallback<Waypoint>() {
        @Override
        public boolean areItemsTheSame(@NonNull Waypoint o, @NonNull Waypoint n) {
            return Objects.equals(o.getId(), n.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Waypoint o, @NonNull Waypoint n) {
            return o.equals(n); // Implement equals/hashCode on Waypoint.
        }
    };

    //────────────────────────────────────────────────── Lifecycle helpers
    private void loadWaypointsAsync() {
        ioExecutor.execute(() -> {
            liveWaypoints.postValue(loadWaypoints());
            waypointsLoaded.postValue(true);
        });
    }

    private void scheduleAutosave() {
        if (autosaveFuture != null && !autosaveFuture.isDone()) autosaveFuture.cancel(false);
        autosaveFuture = ioExecutor.schedule(this::saveWaypoints, 1, TimeUnit.SECONDS);
    }

    //────────────────────────────────────────────────── ItemTouchHelperAdapter
    @Override
    public void onItemMove(int fromPos, int toPos) {
        List<Waypoint> copy = new ArrayList<>(getCurrentList());
        Waypoint w = copy.remove(fromPos);
        copy.add(toPos, w);
        liveWaypoints.setValue(new ArrayList<>(copy));   // triggers observer → submitList
    }

    //────────────────────────────────────────────────── ViewHolder plumbing
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_camera_waypoint_item, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder h, int position) {
        // getItem() is O(1) with ListAdapter
        Waypoint item = getItem(position);
        bindRow(h, item);
    }

    //────────────────────────────────────────────────── Row binding extracted for clarity
    @SuppressLint({"DefaultLocale", "NonConstantResourceId"})
    private void bindRow(@NonNull ItemViewHolder holder, @NonNull Waypoint item) {
        // Tint by state
        holder.layout.setBackgroundColor(ContextCompat.getColor(context,
                item.isActive() ? R.color.waypoint_active : R.color.waypoint_inactive));

        holder.imageWaypoint.setImageBitmap(item.getWaypointImage());
        holder.textDwellTime.setText(String.format("%.1fs", item.dwellTimeMs / 1000.0));

        // Goto / long‑press config
        holder.buttonGoTo.setOnClickListener(v -> waypointsProcessor.goToWaypoint(holder.getBindingAdapterPosition()));
        holder.buttonGoTo.setOnLongClickListener(v -> {
            showEditDialog(item);
            return true;
        });

        // Angle‑speed toggle (unchanged logic)
        holder.toggleWaypointSpeed.clearOnButtonCheckedListeners();
        holder.toggleWaypointSpeed.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return; // ignore un‑checks
            switch (checkedId) {
                case R.id.buttonWaypointSpeedSlow:
                    item.setAngleSpeed(4);
                    break;
                case R.id.buttonWaypointSpeedMed:
                    item.setAngleSpeed(15);
                    break;
                case R.id.buttonWaypointSpeedFast:
                    item.setAngleSpeed(40);
                    break;
                default:
                    break;
            }
            liveWaypoints.setValue(new ArrayList<>(getCurrentList()));
        });

        holder.buttonDelete.setOnClickListener(v -> delete(holder.getBindingAdapterPosition()));
        updateTexts(holder, item);
    }

    private void delete(int pos) {
        waypointsProcessor.cancelIfActive(pos);
        List<Waypoint> copy = new ArrayList<>(getCurrentList());
        if (pos >= 0 && pos < copy.size()) copy.remove(pos);
        liveWaypoints.setValue(new ArrayList<>(copy));
    }

    //────────────────────────────────────────────────── UI helpers (unchanged)
    private void updateTexts(ItemViewHolder h, Waypoint w) {
        double pan = w.getPanAngle(), tilt = w.getTiltAngle();
        Double fp = w.getFocusPoint();
        h.buttonGoTo.setText(fp == null ?
                String.format("%.1f° / %.1f° / -.-%%", pan, tilt) :
                String.format("%.1f° / %.1f° / %.1f%%", pan, tilt, fp));
        if (w.getAngleSpeed() < 10) {
            Button b = h.toggleWaypointSpeed.findViewById(R.id.buttonWaypointSpeedSlow);
            b.setText(w.getAngleSpeed() == 4 ? "Slow" : "Slo+");
            h.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedSlow);
        } else if (w.getAngleSpeed() < 20) {
            h.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedMed);
        } else {
            h.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedFast);
        }
    }

    //────────────────────────────────────────────────── JSON persistence (moved off‑main)
    private void saveWaypoints() {
        SharedPreferences sp = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter()).create();
        String json = gson.toJson(new ArrayList<>(getCurrentList()));
        sp.edit().putString(PREF_KEY_WAYPOINT_LIST, json).apply();
        Log.d(TAG, "Waypoints saved (" + getCurrentList().size() + ")");
    }

    private ArrayList<Waypoint> loadWaypoints() {
        SharedPreferences sp = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
        String json = sp.getString(PREF_KEY_WAYPOINT_LIST, null);
        if (json == null) return new ArrayList<>();
        Gson gson = new GsonBuilder().registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter()).create();
        Type lt = new TypeToken<List<Waypoint>>() {
        }.getType();
        return gson.fromJson(json, lt);
    }

    //────────────────────────────────────────────────── ViewHolder
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final View layout;
        final MaterialButtonToggleGroup toggleWaypointSpeed;
        final ImageView imageWaypoint;
        final TextView textDwellTime;
        final Button buttonDelete;
        final Button buttonGoTo;

        ItemViewHolder(View v) {
            super(v);
            imageWaypoint = v.findViewById(R.id.imageWaypoint);
            toggleWaypointSpeed = v.findViewById(R.id.toggleButtonWaypointSpeed);
            buttonDelete = v.findViewById(R.id.deleteButton);
            buttonGoTo = v.findViewById(R.id.buttonGoToWaypoint);
            textDwellTime = v.findViewById(R.id.textDwellTime);
            layout = v.findViewById(R.id.linearLayoutWaypointControl);
        }
    }

    //────────────────────────────────────────────────── Dialog builder (identical but extracted)
    private void showEditDialog(@NonNull Waypoint item) {
        TableLayout table = new TableLayout(context);
        table.setStretchAllColumns(true);
        table.setPadding(50, 40, 50, 10);
        table.setGravity(Gravity.CENTER);

        EditText dwell = buildInput(String.valueOf(item.dwellTimeMs), InputType.TYPE_CLASS_NUMBER);
        EditText pan = buildInput(String.format("%.2f", item.anglePan), InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
        EditText tilt = buildInput(String.format("%.2f", item.angleTilt), InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);

        table.addView(createRow("Dwell time (ms):", dwell));
        table.addView(createRow("Pan angle (°):", pan));
        table.addView(createRow("Tilt angle (°):", tilt));

        new AlertDialog.Builder(context)
                .setTitle("Set Waypoint Parameters")
                .setView(table)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        item.dwellTimeMs = Integer.parseInt(dwell.getText().toString());
                        item.anglePan = Double.parseDouble(pan.getText().toString());
                        item.angleTilt = Double.parseDouble(tilt.getText().toString());
                        liveWaypoints.setValue(new ArrayList<>(getCurrentList()));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid input", e);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private EditText buildInput(String prefilled, int inputType) {
        EditText e = new EditText(context);
        e.setInputType(inputType);
        e.setText(prefilled);
        return e;
    }

    private TableRow createRow(String label, EditText input) {
        TableRow row = new TableRow(context);
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        tv.setPadding(0, 10, 10, 50);
        row.addView(tv);
        input.setBackground(null);
        input.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        input.setPadding(10, 0, 10, 50);
        row.addView(input);
        return row;
    }
}
