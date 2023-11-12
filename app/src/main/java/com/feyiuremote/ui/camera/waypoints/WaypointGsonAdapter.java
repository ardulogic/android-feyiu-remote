package com.feyiuremote.ui.camera.waypoints;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WaypointGsonAdapter extends TypeAdapter<Waypoint> {
    @Override
    public void write(JsonWriter out, Waypoint waypoint) throws IOException {
        out.beginObject();
        out.name("id").value(waypoint.getId());
        out.name("waypointImage").value(encodeBitmap(waypoint.getWaypointImage()));
        out.name("anglePan").value(waypoint.getPanAngle());
        out.name("angleTilt").value(waypoint.getTiltAngle());
        out.name("panSpeed").value(waypoint.getPanSpeed());
        out.name("tiltSpeed").value(waypoint.getTiltSpeed());
        out.name("angleSpeed").value(waypoint.getAngleSpeed());
        out.name("dwellTimeMs").value(waypoint.getDwellTimeMs());

        if (waypoint.hasFocusPoint()) {
            out.name("focusPoint").value(waypoint.getFocusPoint());
        }

        out.endObject();
    }

    public String encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        // Step 2: Convert the Compressed Bitmap to a Byte Array
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Step 3: Encode the Byte Array to a String
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public Bitmap decodeBitmap(String string) {
        byte[] decodedByteArray = Base64.decode(string, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }

    @Override
    public Waypoint read(JsonReader in) throws IOException {
        Waypoint waypoint = new Waypoint();

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "id":
                    waypoint.setId(in.nextString());
                    break;
                case "waypointImage":
                    String encodedImage = in.nextString();
                    waypoint.setImage(decodeBitmap(encodedImage));
                    break;
                case "anglePan":
                    waypoint.anglePan = (float) in.nextDouble();
                    break;
                case "angleTilt":
                    waypoint.angleTilt = (float) in.nextDouble();
                    break;
                case "panSpeed":
                    waypoint.panSpeed = (float) in.nextDouble();
                    break;
                case "tiltSpeed":
                    waypoint.tiltSpeed = (float) in.nextDouble();
                    break;
                case "angleSpeed":
                    waypoint.angleSpeed = in.nextInt();
                    break;
                case "dwellTimeMs":
                    waypoint.dwellTimeMs = in.nextInt();
                    break;
                case "focusPoint":
                    waypoint.focusPoint = in.nextDouble();
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return waypoint;
    }
}
