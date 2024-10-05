package com.feyiuremote.libs.LiveStream.LiveView.Listeners;

public interface IRectangleDrawTouchListener {

    public void onClear();

    public void onLongPress();

    public void onDoubleTap();

    public void onTooSmallRectangle();

    public void onDrawingRectangle(int x1, int y1, int x2, int y2);

    void onNewRectangle(int rectX1, int rectY1, int rectX2, int rectY2, int drawAreaW, int drawAreaH);
}
