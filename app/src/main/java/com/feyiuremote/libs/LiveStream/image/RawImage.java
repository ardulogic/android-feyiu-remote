package com.feyiuremote.libs.LiveStream.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class RawImage {
    private byte[] imageData;
    private Integer rotationDegrees;

    public RawImage(byte[] imageData, Integer rotationDegrees) {
        this.imageData = imageData;
        this.rotationDegrees = rotationDegrees;
    }

    public Integer getRotationDegrees() {
        return rotationDegrees == null ? 0 : rotationDegrees;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public int getImageDataSize() {
        return this.imageData.length;
    }

    public Bitmap toBitmap() {
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate((float) this.getRotationDegrees());
        Bitmap b = BitmapFactory.decodeByteArray(this.getImageData(), 0, this.getImageDataSize());

        if (b != null) {
            b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rotationMatrix, true);
            System.gc(); // Collect garbage
        }

        return b;
    }
}
