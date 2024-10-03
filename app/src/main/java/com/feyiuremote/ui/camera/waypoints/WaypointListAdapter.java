package com.feyiuremote.ui.camera.waypoints;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.feyiuremote.R;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.CameraViewModel;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;


public class WaypointListAdapter extends RecyclerView.Adapter<WaypointListAdapter.ItemViewHolder>
        implements ItemTouchHelperAdapter {
    private final String TAG = WaypointListAdapter.class.getSimpleName();
    private static final String PREF_KEY_WAYPOINT_LIST = "saved_waypoints";

    private final Context context;
    private final GimbalWaypointsProcessor waypointsProcessor;
    private final LifecycleOwner lifecycleOwner;
    private final MutableLiveData<ArrayList<Waypoint>> waypointList;

    private final MutableLiveData<Boolean> waypointsLoaded;

    private final ArrayList<Waypoint> localWaypointList = new ArrayList<>();

    private boolean loading = false;

    private boolean waiting = false;

    private ScheduledFuture<?> autosaveScheduledFuture;

    @SuppressLint("NotifyDataSetChanged")
    public WaypointListAdapter(Context context, LifecycleOwner lifecycleOwner, CameraViewModel cameraModel, GimbalWaypointsProcessor mWaypointProcessor) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.waypointList = cameraModel.waypointList;
        this.waypointsLoaded = cameraModel.waypointsLoaded;
        this.waypointsProcessor = mWaypointProcessor;

        cameraModel.waypointList.observe(lifecycleOwner, updatedWaypoints -> {
            if (Boolean.TRUE.equals(cameraModel.waypointsLoaded.getValue())) {
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
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new WaypointDiffCallback(localWaypointList, waypointList.getValue()));

        localWaypointList.clear();
        for (Waypoint waypoint : waypointList.getValue()) {
            localWaypointList.add(new Waypoint(waypoint)); // Assuming Waypoint has a copy constructor
        }

        diffResult.dispatchUpdatesTo(this);
    }

    private void waitForFragment() {
        waiting = true;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            waiting = false;
            triggerWaypointObserver();
        }, 500, TimeUnit.MILLISECONDS);
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

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Waypoint item = waypointList.getValue().get(holder.getBindingAdapterPosition());

        if (item.isActive()) {
            holder.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.waypoint_active));
        } else {
            holder.layout.setBackgroundColor(ContextCompat.getColor(context, R.color.waypoint_inactive));
        }

        holder.imageWaypoint.setImageBitmap(item.getWaypointImage());

        holder.buttonGoTo.setOnClickListener(view -> {
            waypointsProcessor.goToWaypoint(holder.getBindingAdapterPosition());
            Log.d("WaypointListAdapter", "Target has been set");
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
        waypointList.postValue(waypointList.getValue());
    }

    private void setTextBoxes(ItemViewHolder holder, Waypoint item) {
        double panAngle = item.getPanAngle();
        double tiltAngle = item.getTiltAngle();
        double angleSpeed = item.getAngleSpeed();
        Double focusPoint = item.getFocusPoint();

        @SuppressLint("DefaultLocale") String s = (focusPoint != null) ?
                String.format("%.1f° / %.1f° / %.1f%%", panAngle, tiltAngle, focusPoint) :
                String.format("%.1f° / %.1f° / -.-%%", panAngle, tiltAngle);

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


        Button buttonDelete;
        Button buttonGoTo;

        public ItemViewHolder(View itemView) {
            super(itemView);

            imageWaypoint = itemView.findViewById(R.id.imageWaypoint);
            toggleWaypointSpeed = itemView.findViewById(R.id.toggleButtonWaypointSpeed);
            buttonDelete = itemView.findViewById(R.id.deleteButton);
            buttonGoTo = itemView.findViewById(R.id.buttonGoToWaypoint);
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
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldList.get(oldItemPosition).getId(), newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            boolean isSame = Objects.equals(oldList.get(oldItemPosition).getId(), newList.get(newItemPosition).getId()) &&
                    (oldList.get(oldItemPosition).angleSpeed == newList.get(newItemPosition).angleSpeed) &&
                    (oldList.get(oldItemPosition).isActive() == newList.get(newItemPosition).isActive());

            return isSame;
        }
    }

}