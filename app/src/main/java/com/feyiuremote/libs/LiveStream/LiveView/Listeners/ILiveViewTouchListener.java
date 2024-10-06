package com.feyiuremote.libs.LiveStream.LiveView.Listeners;

public interface ILiveViewTouchListener {


    public void onLongPress();

    public void onDoubleTap();

    public void onDrawingRectangleFail();

    public void onDrawingRectangle(int x1, int y1, int x2, int y2, int drawAreaW, int drawAreaH);

    void onNewRectangle(int x1, int y1, int x2, int y2, int drawAreaW, int drawAreaH);
}
