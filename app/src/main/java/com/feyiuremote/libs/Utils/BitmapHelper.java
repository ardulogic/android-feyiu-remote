package com.feyiuremote.libs.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapHelper {
    private Context context;


    public static boolean saveBitmap(Context context, Bitmap bitmap, String fileName, String folderName) {
        File folder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Bitmap getBitmapFromResource(Context context, int resId) {
        // resId is the resource ID of the image file
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // You can change the config as needed
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
        return bitmap;
    }
}

