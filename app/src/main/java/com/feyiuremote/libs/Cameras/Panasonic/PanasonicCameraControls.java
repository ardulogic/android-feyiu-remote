package com.feyiuremote.libs.Cameras.Panasonic;

import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraControls;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Cameras.abstracts.State.Camera;
import com.feyiuremote.libs.Utils.SimpleHttpClient;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class PanasonicCameraControls extends CameraControls {

    final String clientName = "FeyiuRemote";
    private final PanasonicCamera camera;

    public PanasonicCameraControls(ExecutorService executor, PanasonicCamera panasonicCamera) {
        super(executor);
        this.camera = panasonicCamera;
    }

    public void enable(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String ctrl_url = camera.state.getBaseUrl() + "cam.cgi?mode=accctrl&type=req_acc&value=" + UUID.randomUUID().toString() + "&value2=" + clientName;
                String reply = null;

                while (reply == null || reply.contains("ok_under_research_no_msg")) {
                    reply = SimpleHttpClient.httpGet(ctrl_url, 1000);

                    if (reply.contains("ok") || reply.contains("err_non_support")) {
                        listener.onSuccess();
                        break;
                    }
                }
            }
        });
    }

    public void recMode(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=camcmd&value=recmode";
                String reply = SimpleHttpClient.httpGet(url, 2000);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }
        });
    }

    public void startStream(ICameraControlListener listener) {
        this.recMode(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String url = camera.state.getBaseUrl() + "cam.cgi?mode=startstream&value=" + PanasonicCameraLiveView.PORT;
                        String reply = SimpleHttpClient.httpGet(url, 2000);

                        if (reply.contains("<result>ok</result>")) {
                            listener.onSuccess();
                        } else {
                            listener.onFailure();
                        }
                    }
                });
            }

            @Override
            public void onFailure() {
                listener.onFailure();
            }
        });
    }

    public void stopStream(ICameraControlListener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String url = camera.state.getBaseUrl() + "cam.cgi?mode=stopstream";
                String reply = SimpleHttpClient.httpGet(url, 1000);

                if (reply.contains("<result>ok</result>")) {
                    listener.onSuccess();
                } else {
                    listener.onFailure();
                }
            }
        });
    }

}
