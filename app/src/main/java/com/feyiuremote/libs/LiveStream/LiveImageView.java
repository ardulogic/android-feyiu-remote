package com.feyiuremote.libs.LiveStream;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.feyiuremote.R;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;

import static java.lang.Math.min;

public class LiveImageView extends View {

    private static final String TAG = LiveImageView.class.getSimpleName();
    private ILiveFeedReceiver mFeedReceiver;
    private Context mContext;

    Bitmap imageBitmap;

    public LiveImageView(Context context) {
        super(context);
        
        mContext = context;
        imageBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.video_unavailable);
    }

    public LiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLiveFeedReceiver(ILiveFeedReceiver receiver) {
        this.mFeedReceiver = receiver;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (canvas == null || mFeedReceiver == null) {
            Log.w(TAG, "Canvas or feed receiver is not ready/set");
            return;
        }

        canvas.drawARGB(255, 0, 0, 0);
        drawImage(canvas, 0);
    }

    private void drawImage(Canvas canvas, Integer rotationDegrees) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        Paint paint = new Paint(Color.GREEN);
        paint.setStrokeWidth(1.0f);
        paint.setStyle(Paint.Style.STROKE);

        imageBitmap = mFeedReceiver.getImage(0);
        Integer width = imageBitmap.getWidth();
        Integer height = imageBitmap.getHeight();
        Rect imageRect = new Rect(0, 0, width, height);

        RectF viewRect = createRootRect(canvas, imageBitmap, rotationDegrees);
        canvas.drawBitmap(imageBitmap, imageRect, viewRect, paint);
    }

    private RectF createRootRect(Canvas canvas, Bitmap bitmapToShow, Integer rotationDegrees) {
        if (bitmapToShow == null) {
            return new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        }
        
        Integer srcWidth;
        Integer srcHeight;

        if ((rotationDegrees == 0) || (rotationDegrees == 180)) {
            srcWidth = bitmapToShow.getWidth();
            srcHeight = bitmapToShow.getHeight();
        } else {
            srcWidth = bitmapToShow.getHeight();
            srcHeight = bitmapToShow.getWidth();
        }

        int maxWidth = canvas.getWidth();
        int maxHeight = canvas.getHeight();
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        float widthRatio = maxWidth / (float) srcWidth;
        float heightRatio = maxHeight / (float) srcHeight;
        float smallRatio = min(widthRatio, heightRatio);

        Float dstWidth;
        Float dstHeight;

        if (widthRatio < heightRatio) {
            dstWidth = (float) maxWidth;
            dstHeight = (smallRatio * srcHeight);
        } else {
            dstHeight = (float) maxHeight;
            dstWidth = (smallRatio * srcWidth);
        }
        float halfWidth = dstWidth * 0.5f;
        float halfHeight = dstHeight * 0.5f;

        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return new RectF(
                    (centerX - halfWidth),
                    (centerY - halfHeight),
                    ((centerX - halfWidth) + dstWidth),
                    ((centerY - halfHeight) + dstHeight)
            );
        } else {
            return new RectF(
                    (centerX - halfHeight),
                    (centerY - halfWidth),
                    ((centerX - halfHeight) + dstHeight),
                    ((centerY - halfWidth) + dstWidth)
            );
        }
    }

    public void refresh() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

}
