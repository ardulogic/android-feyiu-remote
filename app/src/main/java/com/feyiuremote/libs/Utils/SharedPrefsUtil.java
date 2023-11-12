package com.feyiuremote.libs.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class SharedPrefsUtil {

    // Define a key for your preference

    // Save a list of Parcelable objects to SharedPreferences
    public static <T extends Parcelable> void saveListToSharedPreferences(Context context, List<T> parcelableList, String key) {
        SharedPreferences preferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String base64String = toBase64(parcelableList);
        editor.putString(key, base64String);
        editor.apply();
    }

    // Retrieve a list of Parcelable objects from SharedPreferences
    public static <T extends Parcelable> List<T> getListFromSharedPreferences(Context context, Class<T> creatorClass, String key) {
        SharedPreferences preferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String base64String = preferences.getString(key, null);
        if (base64String != null) {
            return fromBase64(base64String, creatorClass);
        }
        return null;
    }

    // Convert a list of Parcelable objects to a Base64-encoded string
    private static <T extends Parcelable> String toBase64(List<T> parcelableList) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Parcel parcel = Parcel.obtain();
        parcel.writeTypedList(parcelableList);
        try {
            byteArrayOutputStream.write(parcel.marshall());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
        }
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    // Convert a Base64-encoded string back to a list of Parcelable objects
    private static <T extends Parcelable> List<T> fromBase64(String base64, Class<T> creatorClass) {
        byte[] byteArray = Base64.decode(base64, Base64.DEFAULT);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);

        Creator<T> creator = new Creator<T>() {
            @Override
            public T createFromParcel(Parcel source) {
                try {
                    return creatorClass.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public T[] newArray(int size) {
                return (T[]) new Parcelable[size];
            }
        };

        return parcel.createTypedArrayList(creator);
    }
}
