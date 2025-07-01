package com.feyiuremote.ui.camera.waypoints;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.feyiuremote.R;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.fragments.waypoints.CameraWaypointsViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;


public class WaypointListAdapter extends RecyclerView.Adapter<WaypointListAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {
    private final String TAG = WaypointListAdapter.class.getSimpleName();
    private final Context context;
    private final GimbalWaypointsProcessor waypointsProcessor;
    private final WaypointsList waypointList;
    private RecyclerView recyclerView;

    @SuppressLint("NotifyDataSetChanged")
    public WaypointListAdapter(Context context, LifecycleOwner lifecycleOwner, CameraWaypointsViewModel viewModel) {
        this.context = context;
        this.waypointList = viewModel.waypointList;
        this.waypointsProcessor = viewModel.processor.getValue();

        waypointList.addListener("waypoint_list_adapter", new WaypointsList.Listener() {
            @Override
            public void onInsert(int index) {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemInserted(index));
                }
            }

            @Override
            public void onRemove(int index) {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemRemoved(index));
                }
            }

            @Override
            public void onUpdate(int index) {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemChanged(index));
                }
            }

            @Override
            public void onMove(int fromIndex, int toIndex) {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemMoved(fromIndex, toIndex));
                }
            }

            @Override
            public void onFullChange() {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyDataSetChanged());
                }
            }
        });
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
        Waypoint item = waypointList.get(holder.getBindingAdapterPosition());

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

            CheckBox trackPoseCheckbox = new CheckBox(context);
            trackPoseCheckbox.setText("Track Pose on Dwell");
            trackPoseCheckbox.setChecked(item.getTrackPoseOnDwell());  // Assuming getter exists
            trackPoseCheckbox.setPadding(10, 30, 10, 10);

            // Add rows to the table
            tableLayout.addView(createTableRow("Dwell time (ms):", dwellTimeInput));
            tableLayout.addView(createTableRow("Pan angle (°):", panAngleInput));
            tableLayout.addView(createTableRow("Tilt angle (°):", tiltAngleInput));
            tableLayout.addView(trackPoseCheckbox);

            // Build and show AlertDialog
            new AlertDialog.Builder(context)
                    .setTitle("Set Waypoint Parameters")
                    .setView(tableLayout)
                    .setPositiveButton("OK", (dialog, whichButton) -> {
                        try {
                            int dwellTime = Integer.parseInt(dwellTimeInput.getText().toString());
                            double panAngle = Double.parseDouble(panAngleInput.getText().toString());
                            double tiltAngle = Double.parseDouble(tiltAngleInput.getText().toString());
                            boolean trackPoseOnDwell = trackPoseCheckbox.isChecked();

                            item.setDwellTime(dwellTime);
                            item.setPanAngle(panAngle);
                            item.setTiltAngle(tiltAngle);
                            item.setTrackPoseOnDwell(trackPoseOnDwell);  // Assuming setter exists

                            waypointList.onWaypointDataChanged(item);

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
                waypointList.onWaypointDataChanged(item);

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
            waypointList.remove(position);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Error deleting item, index out of bounds!");
        }
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
        return waypointList.getAll().size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        waypointList.move(fromPosition, toPosition);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
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

}