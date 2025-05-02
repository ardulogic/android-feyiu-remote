package com.feyiuremote.libs.Cameras.Panasonic;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicApertures;
import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicFocusSpeeds;
import com.feyiuremote.libs.Cameras.Panasonic.Values.PanasonicShutterSpeeds;
import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraControls;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Utils.HttpClient;
import com.feyiuremote.libs.Utils.NamedThreadFactory;
import com.feyiuremote.libs.Utils.XmlParser;
import com.feyiuremote.libs.Utils.XmlSafeParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class PanasonicCameraControls extends CameraControls {

    final String clientName = "FeyiuRemote";
    private final PanasonicCamera camera;
    private final HttpClient httpClient;

    private boolean executing = false;

    private static final ThreadFactory threadFactory = new NamedThreadFactory("PanasonicControls");
    public static final ExecutorService executor = Executors.newFixedThreadPool(1, threadFactory); // Single-thread executor
    private final ArrayBlockingQueue<CameraCommand> commandQueue = new ArrayBlockingQueue<>(100);

    public PanasonicCameraControls(Context context, PanasonicCamera panasonicCamera) {
        super(context);
        this.camera = panasonicCamera;
        this.httpClient = new HttpClient(context);
    }

    private HttpClient getHttpClient() {
        return new HttpClient(context);
    }

    private void executeQueuedCommand() {
        CameraCommand command = null;

        try {
            // Blocking call, waits for a command
            command = commandQueue.take();
            if (command != null) {
                command.execute();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not execute queued command! Thread interrupted.");
        }
    }

    private synchronized void executeQueuedCommands() {
        if (!commandQueue.isEmpty() && !executing) {
            executing = true;
            executeQueuedCommand();
            executing = false;

            executeQueuedCommands();
        }
    }

    private synchronized void queueCommand(CameraCommand command) {
        if (commandQueue.size() < 5) {
            commandQueue.add(command);
            executeQueuedCommands();
        } else {
            Log.e(TAG, "Too many queued commands for Panasonic Camera!");
        }
    }

    // Command interface
    interface CameraCommand {
        void execute();
    }

    // Modify existing methods to use the command queue
    public void enable(ICameraControlListener listener) {
        queueCommand(() -> {
            String ctrl_url = camera.state.getBaseUrl() + "cam.cgi?mode=accctrl&type=req_acc&value=" + UUID.randomUUID().toString() + "&value2=" + clientName;
            String reply = httpClient.get(ctrl_url, 1000);

            if (reply != null) {
                if (reply.contains("ok") || reply.contains("err_non_support")) {
                    listener.onSuccess();
                } else if (reply.contains("ok_under_research_no_msg")) {
                    enable(listener);
                    return;
                }
            }

            listener.onFailure();
        });
    }

    public void updateBaseInfo(ICameraControlListener listener) {
        queueCommand(() -> {
            String xml_string = httpClient.get(camera.state.url, -1);

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
        queueCommand(() -> {
            String xml = httpClient.get(
                    camera.state.getBaseUrl() + "cam.cgi?mode=getstate", -1
            );

            if (xml == null || xml.trim().isEmpty()) {
                Log.e(TAG, "Empty response from getstate");
                camera.state.available = false;
                listener.onFailure();
                return;
            }

            ArrayList<String> fields = new ArrayList<String>(Arrays.asList(
                    "batt", "cammode", "remaincapacity", "videoremaincapacity", "rec", "temperature"
            ));

            // Parse only the fields we care about
            Map<String, String> data = XmlParser.parse(xml, fields);

            // Apply each field safely, track if any succeeded
            boolean ok = false;
            ok |= XmlSafeParser.safeApply(data, "batt", v -> camera.state.battery = v);
//            ok |= XmlSafeParser.safeApply(data, "cammode", v -> camera.state.mode = v);
            ok |= XmlSafeParser.safeParseInt(data, "remaincapacity", v -> camera.state.remainingCapacity = v);
//            ok |= XmlSafeParser.safeParseInt(data, "videoremaincapacity", v -> camera.state.videoCapacity = v);
            ok |= XmlSafeParser.safeParseBool(data, "rec", (b, raw) -> camera.state.isRecording = b);
//            ok |= XmlSafeParser.safeApply(data, "temperature", v -> camera.state.temperature = v);

            camera.state.available = ok;
            if (ok) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void recMode(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=recmode";
            String reply = httpClient.get(url, 2000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void startStream(ICameraControlListener listener) {
        queueCommand(() -> {
            recMode(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    String url = camera.state.getBaseUrl() + "cam.cgi?mode=startstream&value=" + PanasonicCameraLiveView.PORT;
                    String reply = httpClient.get(url, 2000);

                    if (reply != null && reply.contains("<result>ok</result>")) {
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
        });
    }

    public void stopStream(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=stopstream";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void takePicture(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=capture";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void startVideoRecording(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=video_recstart";
            Log.d(TAG, url);
            String reply = getHttpClient().get(url, 3000);

            if (reply != null && reply.contains("<result>ok</result>")) {
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
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=video_recstop";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                camera.state.isRecording = false;
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void keepAlive(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=playmode";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
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
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=capability";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getAllMenuSettings(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=allmenu";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getCurrentMenuSettings(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=curmenu";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void setShutterSpeed(String speed, ICameraControlListener listener) {
        queueCommand(() -> {
            String shutterSpeedValue = PanasonicShutterSpeeds.getControlValue(speed);

            if (shutterSpeedValue != null) {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=shtrspeed&shutter=" + shutterSpeedValue;
                String reply = httpClient.get(url, 1000);

                if (reply != null && reply.contains("<result>ok</result>")) {
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
        queueCommand(() -> {
            String value = PanasonicApertures.getControlValue(aperture);

            if (value != null) {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=setsetting&type=focal&value=" + value;
                String reply = httpClient.get(url, 1000);

                if (reply != null && reply.contains("<result>ok</result>")) {
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
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "cam.cgi?mode=getinfo&type=lens";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getFocusState(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=focus&value=wide-normal";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void getFocusMode(ICameraControlListener listener) {
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=getsetting&type=focusmode";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        });
    }

    public void focus(int speed, IPanasonicCameraFocusControlListener listener) {
        queueCommand(() -> {
            String value = PanasonicFocusSpeeds.getControlValue(speed);
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=focus&value=" + value;
            String reply = httpClient.get(url, 1000);

            if (reply != null) {
                String[] parts = reply.split(",");

                try {
                    // Parse the middle and last numbers
                    String status = parts[0];

                    if (status.contains("ok")) {
                        int currentState = Integer.parseInt(parts[1]);
                        int maxState = Integer.parseInt(parts[2]);
                        double percentage = (double) currentState / maxState * 100;

                        Log.d(TAG, "Received : " + currentState + " / " + maxState);
                        if (currentState < maxState) {
                            listener.onSuccess(percentage);
                        } else {
                            listener.onFailure();
                        }

                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Failed to parse focal numbers.");
                }
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
                String reply = httpClient.get(url, 1000);

                if (reply != null && reply.contains("<result>ok</result>")) {
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
                String reply = httpClient.get(url, 1000);

                if (reply != null && reply.contains("<result>ok</result>")) {
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
            String reply = httpClient.get(url, 1000);
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
        queueCommand(() -> {
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=touch_trace&value=continue&value2=" + x + "/" + y;
            String reply = httpClient.get(url, 1000);
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
        queueCommand(() -> {
            //value=continue also exists
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=touch_trace&value=stop&value2=" + x + "/" + y;
            String reply = httpClient.get(url, 1000);
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
        queueCommand(() -> {
            // It stays in lock mode for a bit, you can send "off" to cancel it
            String url = camera.state.getBaseUrl() + "/cam.cgi?mode=camctrl&type=af_ae_lock&value=on";
            String reply = httpClient.get(url, 1000);

            if (reply != null && reply.contains("<result>ok</result>")) {
                listener.onSuccess();
            } else {
                listener.onFailure();
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
