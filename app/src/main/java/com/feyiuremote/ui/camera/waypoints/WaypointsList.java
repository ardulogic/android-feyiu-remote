package com.feyiuremote.ui.camera.waypoints;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaypointsList {

    private final String TAG = WaypointsList.class.getSimpleName();
    private ArrayList<Waypoint> list = new ArrayList<>();
    private static final String PREF_KEY_WAYPOINT_LIST = "saved_waypoints";

    private boolean isLoaded = false;
    private boolean pendingSave = false;


    public void remove(int position) {
        list.remove(position);

        for (Listener listener : listeners.values()) {
            listener.onRemove(position);
        }

        setPendingSave();
    }

    public void move(int fromPosition, int toPosition) {
        Waypoint wp = list.remove(fromPosition);
        list.add(toPosition, wp);

        for (Listener listener : listeners.values()) {
            listener.onMove(fromPosition, toPosition);
        }

        setPendingSave();
    }

    public int size() {
        return this.list.size();
    }

    public void setAllAsInactive() {
        for (Waypoint wp : list) {
            wp.setActive(false);
        }

        for (Listener listener : listeners.values()) {
            listener.onFullChange();
        }
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Waypoint get(int index) {
        return list.get(index);
    }

    public interface Listener {
        void onInsert(int index);

        void onRemove(int index);

        void onUpdate(int index);

        void onMove(int fromIndex, int toIndex);

        void onFullChange();
    }

    private final Map<String, Listener> listeners = new HashMap<>();

    public WaypointsList() {
    }

    public void add(Waypoint w) {
        list.add(w);

        int index = list.size() - 1;
        for (Listener listener : listeners.values()) {
            listener.onInsert(index);
        }

        setPendingSave();
    }

    public void onWaypointIsActiveChanged(Waypoint w) {
        int index = list.indexOf(w);
        if (index != -1) {
            for (Listener listener : listeners.values()) {
                listener.onUpdate(index);
            }
        }
    }

    public void onWaypointDataChanged(Waypoint w) {
        int index = list.indexOf(w);
        if (index != -1) {
            for (Listener listener : listeners.values()) {
                listener.onUpdate(index);
            }
        }

        setPendingSave();
    }

    public void load(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(PREF_KEY_WAYPOINT_LIST, null);

        if (json != null) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter());
            Gson gson = gsonBuilder.create();

            Type listType = new TypeToken<List<Waypoint>>() {
            }.getType();

            Log.d(TAG, "Waypoints have been loaded.");

            set(new ArrayList<>(gson.fromJson(json, listType)));
        }

        isLoaded = true;
        for (Listener listener : listeners.values()) {
            listener.onFullChange();
        }
    }

    public void save(Context context) {
        if (pendingSave) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_KEY_WAYPOINT_LIST, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // Create a Gson instance with the custom type adapter
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Waypoint.class, new WaypointGsonAdapter());
            Gson gson = gsonBuilder.create();

            // Serialize the list to a JSON string
            ArrayList<Waypoint> captured_list = (ArrayList<Waypoint>) list.clone();
            String json = gson.toJson(captured_list);

            // Save the JSON string to SharedPreferences
            editor.putString(PREF_KEY_WAYPOINT_LIST, json);
            editor.apply();

            Log.d(TAG, "Waypoints have been saved.");
            pendingSave = false;
        } else {
            Log.d(TAG, "Waypoints skipped saving.");
        }
    }

    public void set(ArrayList<Waypoint> newList) {
        this.list = newList;

        for (Listener listener : listeners.values()) {
            listener.onFullChange();
        }
    }

    public ArrayList<Waypoint> getAll() {
        return list;
    }

    public void setPendingSave() {
        pendingSave = true;
    }

    public void addListener(String id, Listener listener) {
        listeners.put(id, listener);
    }

    public void removeListener(String id) {
        listeners.remove(id);
    }

}
