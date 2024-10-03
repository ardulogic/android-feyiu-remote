package com.feyiuremote.libs.LiveStream.image;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import static android.graphics.ImageFormat.NV21;

public class BitmapConverter implements ImageAnalysis.Analyzer {

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            @SuppressLint("UnsafeOptInUsageError")
            Image image = imageProxy.getImage();
            if (image != null && image.getPlanes()[1].getPixelStride() == 1) {
                // from I420 format
                convertToBitmapI420(imageProxy, rotationDegrees);
                return;
            }

            if (imageProxy.getFormat() == ImageFormat.YUV_420_888) {

                if (image.getPlanes()[1].getRowStride() != image.getWidth()) {
                    //  Format : NV12, YU12, YV12  YUV_420_888
                    convertToBitmapYUV420888(imageProxy, rotationDegrees);
                    return;
                }
            }
            //convertToBitmapYUV420888(imageProxy, rotationDegrees)
            convertToBitmapYUV420888NV21(imageProxy, rotationDegrees);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Bitmap Conversion - YUV_420_888 : 35
     * @param imageProxy
     * @param rotationDegrees
     * @return
     */
    private RawImage convertToBitmapI420(ImageProxy imageProxy, Integer rotationDegrees) {
        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);

        try {
            int orgIndex = 0;
            int index = ySize;
            while (index < (ySize + uSize + vSize)) {
                nv21[index++] = imageProxy.getPlanes()[2].getBuffer().get(orgIndex);
                nv21[index++] = imageProxy.getPlanes()[1].getBuffer().get(orgIndex);
                orgIndex++;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        YuvImage yuvImage = new YuvImage(nv21, NV21, width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        imageProxy.close();

        return new RawImage(out.toByteArray());
    }


    /**
     * Bitmap Conversion YUV420-88
     *
     * @param imageProxy
     * @param rotationDegrees
     * @return
     */
    private RawImage convertToBitmapYUV420888(ImageProxy imageProxy, Integer rotationDegrees) {
        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        int outputOffset = 0;

        if (imageProxy.getPlanes()[0].getPixelStride() != 1) {
            /////////  Y BUFFER  /////////
            byte[] rowBuffer1 = new byte[imageProxy.getPlanes()[0].getRowStride()];

            for (int row = 0; row < imageProxy.getHeight() / imageProxy.getPlanes()[0].getPixelStride(); row++) {
                yBuffer.position(row * imageProxy.getPlanes()[0].getRowStride());
                yBuffer.get(rowBuffer1, 0, imageProxy.getPlanes()[0].getPixelStride());
                if (outputOffset > 0) {
                    for (int col = 0; col < imageProxy.getWidth(); col++) {
                        nv21[outputOffset] = rowBuffer1[col * imageProxy.getPlanes()[0].getPixelStride()];
                        outputOffset += imageProxy.getPlanes()[0].getPixelStride();
                    }
                }
            }
        } else {
            for (int row = 0; row < imageProxy.getHeight(); row++) {
                yBuffer.position(row * imageProxy.getPlanes()[0].getRowStride());
                yBuffer.get(nv21, outputOffset, imageProxy.getWidth());
                outputOffset += imageProxy.getWidth();
            }
        }

        try {
            /////////  V BUFFER  /////////
            for (int row = 0; row < (imageProxy.getHeight() / imageProxy.getPlanes()[2].getPixelStride()) - 1; row++) {
                vBuffer.position(row * imageProxy.getPlanes()[2].getRowStride());
                vBuffer.get(nv21, outputOffset, imageProxy.getWidth());
                outputOffset += imageProxy.getWidth();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            /////////  U BUFFER  /////////
            for (int row = 0; row < (imageProxy.getHeight() / imageProxy.getPlanes()[1].getPixelStride()) - 1; row++) {
                uBuffer.position(row * imageProxy.getPlanes()[1].getRowStride());
                uBuffer.get(nv21, outputOffset, imageProxy.getWidth());
                outputOffset += imageProxy.getWidth();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        YuvImage yuvImage = new YuvImage(nv21, NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        imageProxy.close();

        return new RawImage(out.toByteArray());
    }

    /**
     * Bitmap Conversion - YUV420-NV21
     * Image format YUV_420_888 : 35
     *
     * @param imageProxy
     * @param rotationDegrees
     * @return
     */
    private RawImage convertToBitmapYUV420888NV21(ImageProxy imageProxy, Integer rotationDegrees) {
        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        YuvImage yuvImage = new YuvImage(nv21, NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        imageProxy.close();

        return new RawImage(out.toByteArray());
    }

}
