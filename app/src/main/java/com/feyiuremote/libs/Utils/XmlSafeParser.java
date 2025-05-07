package com.feyiuremote.libs.Utils;

import android.util.Log;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class for safely extracting and parsing values from an XML-derived Map.
 */
public class XmlSafeParser {
    private static final String TAG = XmlSafeParser.class.getSimpleName();

    /**
     * Fetches a raw String, checks null/empty, then passes it to setter.
     *
     * @param data   Map of tag names to their string values
     * @param key    The XML tag name to extract
     * @param setter Consumer that accepts the raw String
     * @return true if a non-empty value was found (and setter called)
     */
    public static boolean safeApply(
            Map<String, String> data,
            String key,
            Consumer<String> setter) {
        String val = data.get(key);
        if (val == null || val.isEmpty()) {
            Log.w(TAG, "Missing or empty <" + key + "> in camera response");
            return false;
        }
        setter.accept(val);
        return true;
    }

    /**
     * Like safeApply, but parses to int.
     *
     * @param data   Map of tag names to their string values
     * @param key    The XML tag name to extract
     * @param setter Consumer that accepts the parsed Integer
     * @return true if parsing succeeds
     */
    public static boolean safeParseInt(
            Map<String, String> data,
            String key,
            Consumer<Integer> setter) {
        return safeApply(data, key, s -> {
            try {
                setter.accept(Integer.parseInt(s));
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Bad integer for <" + key + ">: " + s, nfe);
            }
        });
    }

    /**
     * Like safeApply, but parses to double.
     *
     * @param data   Map of tag names to their string values
     * @param key    The XML tag name to extract
     * @param setter Consumer that accepts the parsed Double
     * @return true if parsing succeeds
     */
    public static boolean safeParseDouble(
            Map<String, String> data,
            String key,
            Consumer<Double> setter) {
        return safeApply(data, key, s -> {
            try {
                setter.accept(Double.parseDouble(s));
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Bad double for <" + key + ">: " + s, nfe);
            }
        });
    }

    /**
     * Like safeApply, but interprets "off" as false, anything else as true.
     *
     * @param data   Map of tag names to their string values
     * @param key    The XML tag name to extract
     * @param setter BiConsumer that accepts the Boolean state and the raw string
     * @return true if a non-empty value was found (and setter called)
     */
    public static boolean safeParseBool(
            Map<String, String> data,
            String key,
            BiConsumer<Boolean, String> setter) {
        return safeApply(data, key, s -> {
            boolean b = !s.equalsIgnoreCase("off");
            setter.accept(b, s);
        });
    }

    /**
     * Like safeApply, but returns true only if the value matches `onValue`, case-insensitively.
     *
     * @param data    Map of tag names to their string values
     * @param key     The XML tag name to extract
     * @param onValue The string value that represents "true"
     * @param setter  BiConsumer that accepts the Boolean state and the raw string
     * @return true if a non-empty value was found (and setter called)
     */
    public static boolean safeParseBool(
            Map<String, String> data,
            String key,
            String onValue,
            BiConsumer<Boolean, String> setter) {
        return safeApply(data, key, s -> {
            boolean b = s.equalsIgnoreCase(onValue);
            setter.accept(b, s);
        });
    }
}

