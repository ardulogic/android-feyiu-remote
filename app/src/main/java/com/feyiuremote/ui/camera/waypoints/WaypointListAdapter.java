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
import androidx.recyclerview.widget.RecyclerView;

import com.feyiuremote.R;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.models.CameraWaypointsViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class WaypointListAdapter extends RecyclerView.Adapter<WaypointListAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {
    private final String TAG = WaypointListAdapterNew.class.getSimpleName();
    private static final String PREF_KEY_WAYPOINT_LIST = "saved_waypoints";

    private final Context context;
    private final GimbalWaypointsProcessor waypointsProcessor;
    private final MutableLiveData<ArrayList<Waypoint>> waypointList;

    private final MutableLiveData<Boolean> waypointsLoaded;

    private final ArrayList<Waypoint> localWaypointList = new ArrayList<>();
    private final LifecycleOwner lifecycleOwner;

    private boolean loading = false;

    private boolean waiting = false;

    private ScheduledFuture<?> autosaveScheduledFuture;

    private final Handler main = new Handler(Looper.getMainLooper());


    @SuppressLint("NotifyDataSetChanged")
    public WaypointListAdapter(Context context, LifecycleOwner lifecycleOwner, CameraWaypointsViewModel viewModel) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.waypointList = viewModel.waypointList;
        this.waypointsLoaded = viewModel.waypointsLoaded;
        this.waypointsProcessor = viewModel.processor.getValue();

        waypointList.observe(lifecycleOwner, updatedWaypoints -> {
            if (Boolean.TRUE.equals(waypointsLoaded.getValue())) {
                saveWaypointsInBackground();
            }

            if (!loading && !waiting) {
                updateLocalWaypoints();
            }
        });

        if (Boolean.FALSE.equals(waypointsLoaded.getValue())) {
            loadWaypointsInBackground();
        } else {
            waitForFragment();
        }
    }

    private void updateLocalWaypoints() {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new WaypointDiffCallback(localWaypointList, waypointList.getValue()));

        localWaypointList.clear();

        if (waypointList.getValue() != null) {
            for (Waypoint waypoint : waypointList.getValue()) {
                localWaypointList.add(new Waypoint(waypoint)); // Assuming Waypoint has a copy constructor
            }
        }

        diffResult.dispatchUpdatesTo(this);
    }

    private void waitForFragment() {
        waiting = true;
        main.postDelayed(() -> {
            waiting = false;
            triggerWaypointObserver();   // ← already on main
        }, 300);
    }

    private void loadWaypointsInBackground() {
        loading = true;

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            waypointList.postValue(loadWaypoints());
            waypointsLoaded.postValue(true);
            loading = false;
        });
    }

    private void saveWaypointsInBackground() {
        if (autosaveScheduledFuture != null && !autosaveScheduledFuture.isDone()) {
            // Cancel the previous scheduled task if it's still running
            autosaveScheduledFuture.cancel(true);
        }
        autosaveScheduledFuture = Executors.newSingleThreadScheduledExecutor().schedule(this::saveWaypoints, 1000, TimeUnit.MILLISECONDS);
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_camera_waypoint_item, parent, false);
        return new ItemViewHolder(view);
    }

    // Helper method to create a table row with a label and EditText
    TableRow createTableRow(String labelText, EditText editText) {
        TableRow row = new TableRow(context);

        TextView label = new TextView(context);
        label.setText(labelText);
        label.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        label.setPadding(0, 10, 10, 50);

        // Set layout parameters with vertical centering for label
        TableRow.LayoutParams labelParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.CENTER_VERTICAL;
        label.setLayoutParams(labelParams);

        editText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editText.setBackground(null);
        editText.setPadding(10, 0, 10, 50);
        editText.setMinEms(5);
        editText.setTextSize(24);
        editText.setSingleLine(true);

        // Set layout parameters with vertical centering for EditText
        TableRow.LayoutParams editTextParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        editTextParams.gravity = Gravity.CENTER_VERTICAL;
        editText.setLayoutParams(editTextParams);

        row.addView(label);
        row.addView(editText);
        return row;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Waypoint item = waypointList.getValue().get(holder.getBindingAdapterPosition());

        if (item.isActive()) {
            holder.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.waypoint_active));
        } else {
            holder.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.waypoint_inactive));
        }

        holder.imageWaypoint.setImageBitmap(item.getWaypointImage());

        holder.textDwellTime.setText(String.format("%.1fs", item.dwellTimeMs / 1000.0));

        holder.buttonGoTo.setOnClickListener(view -> {
            waypointsProcessor.goToWaypoint(holder.getBindingAdapterPosition());
            Log.d("WaypointListAdapter", "Target has been set");
        });

        // Adding long click listener for dwell time input
        holder.buttonGoTo.setOnLongClickListener(view -> {
            Context context = view.getContext();

            // Create the TableLayout
            TableLayout tableLayout = new TableLayout(context);
            tableLayout.setStretchAllColumns(true);
            tableLayout.setPadding(50, 40, 50, 10);
            tableLayout.setGravity(Gravity.CENTER);

            // Create and pre-populate inputs
            EditText dwellTimeInput = new EditText(context);
            dwellTimeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            dwellTimeInput.setText(String.valueOf(item.dwellTimeMs));

            EditText panAngleInput = new EditText(context);
            panAngleInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
            panAngleInput.setText(String.format("%.2f", item.anglePan));

            EditText tiltAngleInput = new EditText(context);
            tiltAngleInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
            tiltAngleInput.setText(String.format("%.2f", item.angleTilt));

            // Add rows to the table
            tableLayout.addView(createTableRow("Dwell time (ms):", dwellTimeInput));
            tableLayout.addView(createTableRow("Pan angle (°):", panAngleInput));
            tableLayout.addView(createTableRow("Tilt angle (°):", tiltAngleInput));

            // Build and show AlertDialog
            new AlertDialog.Builder(context)
                    .setTitle("Set Waypoint Parameters")
                    .setView(tableLayout)
                    .setPositiveButton("OK", (dialog, whichButton) -> {
                        try {
                            int dwellTime = Integer.parseInt(dwellTimeInput.getText().toString());
                            double panAngle = Double.parseDouble(panAngleInput.getText().toString());
                            double tiltAngle = Double.parseDouble(tiltAngleInput.getText().toString());

                            item.dwellTimeMs = dwellTime;
                            item.anglePan = panAngle;
                            item.angleTilt = tiltAngle;

                            triggerWaypointObserver();
                            Log.d("WaypointListAdapter", "Updated waypoint: Dwell=" + dwellTime + "ms, Pan=" + panAngle + "°, Tilt=" + tiltAngle + "°");
                        } catch (NumberFormatException e) {
                            Log.e("WaypointListAdapter", "Invalid input in waypoint configuration", e);
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, whichButton) -> dialog.dismiss())
                    .show();

            return true;
        });

        holder.toggleWaypointSpeed.clearOnButtonCheckedListeners();
        holder.toggleWaypointSpeed.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // This checks button if you try to uncheck already checked one
            boolean isCheckedTwice = group.getCheckedButtonId() == -1;
            if (isCheckedTwice) {
                group.check(checkedId);
                isChecked = true;
            }

            // NOW IT MAKES SENSE
            // it triggers two times because checking one button unchecks another
            if (isChecked) {
                switch (checkedId) {
                    case R.id.buttonWaypointSpeedSlow:
                        if (isCheckedTwice) {
                            item.setAngleSpeed(item.angleSpeed == 4 ? 2 : 4);
                        } else {
                            item.setAngleSpeed(4);
                        }
                        break;
                    case R.id.buttonWaypointSpeedMed:
                        item.setAngleSpeed(15);
                        break;
                    case R.id.buttonWaypointSpeedFast:
                        item.setAngleSpeed(40);
                        break;
                }

                // This helps from auto-reloading after clicking on speed buttons
                // it makes sense since the update actually is visible after click
                // but this has a sideeffect - it does not save angle changes
                localWaypointList.get(holder.getBindingAdapterPosition()).angleSpeed = item.angleSpeed;
                triggerWaypointObserver();

                // Update texts since speed changed
                setTextBoxes(holder, item);
            }
        });

        holder.buttonDelete.setOnClickListener(view -> {
            this.onItemDelete(holder.getBindingAdapterPosition());
        });

        setTextBoxes(holder, item);
    }

    private void onItemDelete(int position) {
        try {
            waypointsProcessor.cancelIfActive(position);
            waypointList.getValue().remove(position);
            triggerWaypointObserver();
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Error deleting item, index out of bounds!");
        }
    }

    private void triggerWaypointObserver() {
        main.post(() -> {
            waypointList.setValue(waypointList.getValue()); // setValue, not postValue
            updateLocalWaypoints();                         // safe – on main
        });
    }

    private void setTextBoxes(ItemViewHolder holder, Waypoint item) {
        double panAngle = item.getPanAngle();
        double tiltAngle = item.getTiltAngle();
        double angleSpeed = item.getAngleSpeed();
        Double focusPoint = item.getFocusPoint();

        @SuppressLint("DefaultLocale") String s = (focusPoint != null) ? String.format("%.1f° / %.1f° / %.1f%%", panAngle, tiltAngle, focusPoint) : String.format("%.1f° / %.1f° / -.-%%", panAngle, tiltAngle);

        holder.buttonGoTo.setText(s);

        if (angleSpeed < 10) {
            Button buttonWaypointSpeedSlow = holder.toggleWaypointSpeed.findViewById(R.id.buttonWaypointSpeedSlow);
            buttonWaypointSpeedSlow.setText(item.angleSpeed == 4 ? "Slow" : "Slo+");
            holder.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedSlow);
        } else if (angleSpeed < 20) {
            holder.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedMed);
        } else {
            holder.toggleWaypointSpeed.check(R.id.buttonWaypointSpeedFast);
        }
    }

    @Override
    public int getItemCount() {
        if (waiting) {
            return 0;
        }

        return waypointList.getValue().size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Waypoint movedItem = waypointList.getValue().remove(fromPosition);
        waypointList.getValue().add(toPosition, movedItem);
        triggerWaypointObserver();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        View layout;
        MaterialButtonToggleGroup toggleWaypointSpeed;
        ImageView imageWaypoint;
        TextView textDwellTime;
        Button buttonDelete;
        Button buttonGoTo;

        public ItemViewHolder(View itemView) {
            super(itemView);

            imageWaypoint = itemView.findViewById(R.id.imageWaypoint);
            toggleWaypointSpeed = itemView.findViewById(R.id.toggleButtonWaypointSpeed);
            buttonDelete = itemView.findViewById(R.id.deleteButton);
            buttonGoTo = itemView.findViewById(R.id.buttonGoToWaypoint);
            textDwellTime = itemView.findViewById(R.id.textDwellTime);
            layout = itemView.findViewById(R.id.linearLayoutWaypointControl);
        }
    }

    public void saveWaypoints() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Create a Gson instance with the custom type adapter
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter());
        Gson gson = gsonBuilder.create();

        // Serialize the list to a JSON string
        ArrayList<Waypoint> captured_list = (ArrayList<Waypoint>) waypointList.getValue().clone();
        String json = gson.toJson(captured_list);

        // Save the JSON string to SharedPreferences
        editor.putString(PREF_KEY_WAYPOINT_LIST, json);
        editor.apply();

        Log.d(TAG, "Waypoints have been saved.");
    }

    public ArrayList<Waypoint> loadWaypoints() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(PREF_KEY_WAYPOINT_LIST, null);

        if (json != null) {
            // Create a Gson instance with the custom type adapter
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter());
            Gson gson = gsonBuilder.create();

            Type listType = new TypeToken<List<Waypoint>>() {
            }.getType();

            Log.d(TAG, "Waypoints have been loaded.");
            // Deserialize the JSON string back to a list of Waypoint objects
            return gson.fromJson(json, listType);
        }

        return new ArrayList<>();
    }

    public class WaypointDiffCallback extends DiffUtil.Callback {
        private final List<Waypoint> oldList;
        private final List<Waypoint> newList;

        public WaypointDiffCallback(List<Waypoint> oldList, List<Waypoint> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return (newList != null) ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldList.get(oldItemPosition).getId(), newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Waypoint oldItem = oldList.get(oldItemPosition);
            Waypoint newItem = newList.get(newItemPosition);

            return oldItem.angleSpeed == newItem.angleSpeed &&
                    oldItem.dwellTimeMs == newItem.dwellTimeMs &&
                    oldItem.anglePan == newItem.anglePan &&
                    oldItem.angleTilt == newItem.angleTilt &&
                    Objects.equals(oldItem.getId(), newItem.getId());
        }
    }

}