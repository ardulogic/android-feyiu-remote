package com.feyiuremote.libs.Cameras.Panasonic;

import android.net.Network;
import android.util.Log;

import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicApertures;
import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicFocusSpeeds;
import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicShutterSpeeds;
import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraControls;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Utils.SimpleHttpClient;
import com.feyiuremote.libs.Utils.XmlParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class PanasonicCameraControls extends CameraControls {

    final String clientName = "FeyiuRemote";
    private final PanasonicCamera camera;
    private final Network network;

    public PanasonicCameraControls(Network network, PanasonicCamera panasonicCamera) {
        this.camera = panasonicCamera;
        this.network = network;
    }

    public void enable(ICameraControlListener listener) {
        executor.execute(() -> {
            String ctrl_url = camera.state.getBaseUrl() + "cam.cgi?mode=accctrl&type=req_acc&value=" + UUID.randomUUID().toString() + "&value2=" + clientName;
            String reply = null;

            while (reply == null || reply.contains("ok_under_research_no_msg")) {
                reply = SimpleHttpClient.httpGet(ctrl_url, 1000, network);

                if (reply.contains("ok") || reply.contains("err_non_support")) {
                    listener.onSuccess();
                    break;
                }
            }
        });
    }

    public void updateBaseInfo(ICameraControlListener listener) {
        executor.execute(() -> {
            String xml_string = SimpleHttpClient.httpGet(camera.state.url, -1, network);

            if (!xml_string.isEmpty()) {
                ArrayList<String> fields = new ArrayList<String>(Arrays.asList(
                        "friendlyName", "modelNumber", "UDN"
                ));

                Map<String, String> data = XmlParser.parse(xml_string, fields);
                camera.state.name = data.get("friendlyName");
                camera.state.model = data.get("modelNumber");
                camera.state.udn = data.get("UDN");
                camera.state.available = true;

                listener.onSuccess();
            } else {
                camera.state.available = false;
                listener.onFailure();
            }
        });
    }

    public void updateModeState(ICameraControlListener listener) {
        executor.execute(() -> {
            String xml_string = SimpleHttpClient.httpGet(camera.state.getBaseUrl() + "cam.cgi?mode=getstate", -1, network);

            if (!xml_string.isEmpty()) {
                ArrayList<String> fields = new ArrayList<String>(Arrays.asList(
                        "batt", "cammode", "remaincapacity", "videoremaincapacity", "rec", "temperature"
                ));

                Map<String, String> data = XmlParser.parse(xml_string, fields);
                camera.state.battery = data.get("batt");
                camera.state.isRecording = data.get("rec") == "off" ? false : true;
//                    state.mode = data.get("cammode");


//                    state.photoCapacity = Integer.parseInt(Objects.requireNonNull(data.get("remaincapacity")));
//                    state.videoCapacity = Integer.parseInt(Objects.requireNonNull(data.get("videoremaincapacity")));
//                    state.isRecording = !Objects.requireNonNull(data.get("rec")).contains("off");
//                    state.temperature = data.get("temperature");
                camera.state.available = true;

                listener.onSuccess();
            } else {
                camera.state.available = false;
                listener.onFailure();
            }
        });
    }


    public void recMode(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=recmode";
            String reply = SimpleHttpClient.httpGet(url, 2000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void startStream(ICameraControlListener listener) {
        this.recMode(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=startstream&value=" + PanasonicCameraLiveView.PORT;
                String reply = SimpleHttpClient.httpGet(url, 2000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }

            @Override
            public void onFailure() {
                listener.onFailure();
            }
        });
    }

    public void stopStream(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=stopstream";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void takePicture(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=capture";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void startVideoRecording(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=video_recstart";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                camera.state.isRecording = true;
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void toggleVideoRecording(ICameraControlListener listener) {
        if (!camera.state.isRecording) {
            startVideoRecording(listener);
        } else {
            stopVideoRecording(listener);
        }
    }

    public void stopVideoRecording(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=video_recstop";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                camera.state.isRecording = false;
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void keepAlive(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=playmode";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    /**
     * Returns XML of recording resolutions/etc
     *
     * @param listener
     */
    public void getCapability(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=capability";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getAllMenuSettings(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=allmenu";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getCurrentMenuSettings(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=curmenu";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }


    public void setShutter(String shutter, ICameraControlListener listener) {
        executor.execute(() -> {
            String shutterSpeedValue = PanasonicShutterSpeeds.getControlValue(shutter);

            if (shutterSpeedValue != null) {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=shtrspeed&shutter=" + shutterSpeedValue;
                String reply = SimpleHttpClient.httpGet(url, 1000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            } else {
                listener.onFailure(); // Shutter speed not found in the map
            }
        });
    }

    public void setAperture(String aperture, ICameraControlListener listener) {
        executor.execute(() -> {
            String value = PanasonicApertures.getControlValue(aperture);

            if (value != null) {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=setsetting&type=focal&value=" + value;
                String reply = SimpleHttpClient.httpGet(url, 1000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            } else {
                listener.onFailure(); // Shutter speed not found in the map
            }
        });
    }

    // Response is: ok,2304/256,1273/256,3328/256,16384/256,0,off,42,14,on,128/1024,on
    //              ok,
    //              2304/256, Max f-stop
    //              1273/256, Min f-stop (Changes when zoomed-in)
    //              3328/256, Max shutter
    //              16384/256, Min shutter
    //              0, ?
    //              off, ?
    //              42, Max zoom
    //              14, Min Zoom
    //              on, ? - ois?
    //              128/1024, ?
    //              on ?
    //
    // Zoom: (Max 1273/256 (4.98), Min 925/256 (3.61))
    // After changing focus:
    // NOTE IT DOES NOT WORK WITH MANUAL FOCUS MODE, WONT UPDATE!!!!
    public void getLensInfo(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=lens";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getFocusState(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=focus&value=wide-normal";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getFocusMode(ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=getsetting&type=focusmode";
            String reply = SimpleHttpClient.httpGet(url, 1000, network);

            if (reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void focus(int speed, IPanasonicCameraFocusControlListener listener) {
        executor.execute(() -> {
            String value = PanasonicFocusSpeeds.getControlValue(speed);
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=focus&value=" + value;
            String reply = SimpleHttpClient.httpGet(url, 1000, network);
            String[] parts = reply.split(",");

            try {
                // Parse the middle and last numbers
                String status = parts[0];

                if (status.contains("ok")) {
                    int currentState = Integer.parseInt(parts[1]);
                    int maxState = Integer.parseInt(parts[2]);
                    double percentage = (double) currentState / maxState * 100;
                    listener.onSuccess(percentage);
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Failed to parse focal numbers.");
            }

            listener.onFailure();
        });
    }

    public void autoFocus(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // It stays in lock mode for a bit, you can send "off" to cancel it
                String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=focus&value=on";
                String reply = SimpleHttpClient.httpGet(url, 1000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }
        });
    }

    public void autoFocusMf(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // It stays in lock mode for a bit, you can send "off" to cancel it
                String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camcmd&value=oneshot_af";
                String reply = SimpleHttpClient.httpGet(url, 1000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }
        });
    }

    /**
     * Starts touch trace
     *
     * @param x_center_perc Example: 0.5
     * @param y_center_perc Example: 0.5
     * @param listener
     */
    public void touchFocus(double x_center_perc, double y_center_perc, ICameraControlListener listener) {
        int x_start = Math.max(0, (int) Math.round(1024 * x_center_perc));
        int y_start = Math.max(0, (int) Math.round(1024 * y_center_perc));

        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=touch_trace&value=start&value2=" + x_start + "/" + y_start;
            String reply = SimpleHttpClient.httpGet(url, 1000, network);
            Log.d("POI", url);

            if (reply.contains("ok")) {
                touchTraceContinue(x_start, y_start, new ICameraControlListener() {
                    @Override
                    public void onSuccess() {
                        touchTraceStop(x_start, y_start, listener);
                    }

                    @Override
                    public void onFailure() {
                        listener.onFailure();
                    }
                });
            } else {
                Log.d("POI", reply);
                listener.onFailure();
            }
        });
    }

    /**
     * Starts touch trace
     *
     * @param x        Example: 0.5
     * @param y        Example: 0.5
     * @param listener
     */
    public void touchTraceContinue(int x, int y, ICameraControlListener listener) {
        executor.execute(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=touch_trace&value=continue&value2=" + x + "/" + y;
            String reply = SimpleHttpClient.httpGet(url, 1000, network);
            Log.d("POI", url);
            Log.d("POI", "Reply:" + reply);

            if (reply.contains("ok")) {
                listener.onSuccess();
            } else {
                Log.d("POI", reply);
                listener.onFailure();
            }
        });
    }

    /**
     * Starts touch trace (there is also option to continue)
     *
     * @param x        Example: 0.5
     * @param y        Example: 0.5
     * @param listener
     */
    public void touchTraceStop(int x, int y, ICameraControlListener listener) {
        executor.execute(() -> {
            //value=continue also exists
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=touch_trace&value=stop&value2=" + x + "/" + y;
            String reply = SimpleHttpClient.httpGet(url, 1000, network);
            Log.d("POI", url);
            Log.d("POI", "Reply:" + reply);

            if (reply.contains("ok")) {
                listener.onSuccess();
            } else {
                Log.d("POI", reply);
                listener.onFailure();
            }
        });
    }


    /**
     * For some fucking reason you cant just adjust focus without AEL
     * First you need to call lock AEL and then focus
     *
     * @param listener
     */
    public void lockAEL(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // It stays in lock mode for a bit, you can send "off" to cancel it
                String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=af_ae_lock&value=on";
                String reply = SimpleHttpClient.httpGet(url, 1000, network);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }
        });
    }


    // Set Focus
//    cam.cgi?mode=accctrl&type=req_acc&value=1000
//    cam.cgi?mode=camcmd&value=recmode
//    cam.cgi?mode=camctrl&type=af_ae_lock&value=on
//    cam.cgi?mode=camctrl&type=asst_disp&value=current_auto&value2=mf_asst/0/0    Response: ok,pinp,300,501,501
//    cam.cgi?mode=camctrl&type=focus&value=wide-normal // Aparently this should respond with focus position!
}
