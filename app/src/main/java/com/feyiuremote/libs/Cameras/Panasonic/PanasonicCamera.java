package com.feyiuremote.libs.Cameras.Panasonic;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Cameras.abstracts.State.Camera;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.Utils.SimpleHttpClient;
import com.feyiuremote.libs.Utils.XmlParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;


public class PanasonicCamera extends Camera {

    public PanasonicCameraControls controls;
    public PanasonicCameraLiveView live;
    public PanasonicCameraState state;

    public PanasonicCamera(ExecutorService executor, String ddUrl) {
        super(executor);

        this.state = new PanasonicCameraState(ddUrl);
        this.controls = new PanasonicCameraControls(executor, this);
    }

    public void createLiveView(LiveFeedReceiver feedReceiver) {
        this.live = new PanasonicCameraLiveView(executor, this, feedReceiver);
    }

    public PanasonicCameraLiveView getLiveView() {
        return this.live;
    }

    public void updateBaseInfo(ICameraControlListener listener) {
        executor.execute(new Runnable(){
            @Override
            public void run() {
                SimpleHttpClient http = new SimpleHttpClient();
                String xml_string = http.httpGet(state.url, -1);

                if (!xml_string.isEmpty()) {
                    ArrayList<String> fields = new ArrayList<String>(Arrays.asList(
                            "friendlyName", "modelNumber", "UDN"
                    ));

                    Map<String, String> data = XmlParser.parse(xml_string, fields);
                    state.name = data.get("friendlyName");
                    state.model = data.get("modelNumber");
                    state.udn = data.get("UDN");
                    state.available = true;

                    listener.onSuccess();
                } else {
                    state.available = false;
                    listener.onFailure();
                }
            }
        });
    }

    public void updateModeState(ICameraControlListener listener) {
        executor.execute(new Runnable(){
            @Override
            public void run() {
                SimpleHttpClient http = new SimpleHttpClient();
                String xml_string = http.httpGet(state.getBaseUrl() + "cam.cgi?mode=getstate", -1);

                if (!xml_string.isEmpty()) {
                    ArrayList<String> fields = new ArrayList<String>(Arrays.asList(
                            "batt", "cammode", "remaincapacity", "videoremaincapacity", "rec", "temperature"
                    ));

                    Map<String, String> data = XmlParser.parse(xml_string, fields);
//                    state.battery = data.get("batt");
//                    state.mode = data.get("cammode");


//                    state.photoCapacity = Integer.parseInt(Objects.requireNonNull(data.get("remaincapacity")));
//                    state.videoCapacity = Integer.parseInt(Objects.requireNonNull(data.get("videoremaincapacity")));
//                    state.isRecording = !Objects.requireNonNull(data.get("rec")).contains("off");
//                    state.temperature = data.get("temperature");
                    state.available = true;

                    listener.onSuccess();
                } else {
                    state.available = false;
                    listener.onFailure();
                }
            }
        });
    }

    @Override
    public void close() {
        if (this.live != null) {
            this.live.stop();
        }
    }

}
