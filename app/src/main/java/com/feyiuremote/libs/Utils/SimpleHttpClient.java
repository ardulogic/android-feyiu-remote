package com.feyiuremote.libs.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleHttpClient {
    private static final String TAG = SimpleHttpClient.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final int BUFFER_SIZE = 262144;

    @NotNull
    public static String httpGet(@NotNull String url, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        InputStream inputStream = (InputStream)null;
        String replyString = "";
        int timeout = timeoutMs;
        if (timeoutMs < 0) {
            timeout = 10000;
        }

        try {
            URLConnection var10000 = (new URL(url)).openConnection();
            if (var10000 == null) {
                throw new NullPointerException("null cannot be cast to non-null type java.net.HttpURLConnection");
            }

            HttpURLConnection httpConn = (HttpURLConnection)var10000;

            try {
                httpConn.setRequestMethod("GET");
                httpConn.setConnectTimeout(timeout);
                httpConn.setReadTimeout(timeout);
                httpConn.connect();
                int responseCode = httpConn.getResponseCode();
                if (responseCode == 200) {
                    inputStream = httpConn.getInputStream();
                }

                if (inputStream == null) {
                    Log.w(TAG, "httpGet: Response Code Error: " + responseCode + ": " + url);
                    return "";
                }
            } catch (Exception e) {
                Log.w(TAG, "httpGet: " + url + "  " + e.getMessage());
                e.printStackTrace();
                httpConn.disconnect();
                return "";
            }
        } catch (Exception e) {
            Log.w(TAG, "httpGet(2): " + url + "  " + e.getMessage());
            e.printStackTrace();
            return "";
        }

        try {
            StringBuilder responseBuf = new StringBuilder();
            BufferedReader reader = new BufferedReader((Reader)(new InputStreamReader(inputStream)));

            while(true) {
                int newBuffer = reader.read();
                if (newBuffer == -1) {
                    String var26 = responseBuf.toString();
                    Intrinsics.checkNotNullExpressionValue(var26, "responseBuf.toString()");
                    replyString = var26;
                    reader.close();
                    break;
                }

                responseBuf.append((char)newBuffer);
            }
        } catch (Exception var20) {
            Log.w(TAG, "httpGet: exception: " + var20.getMessage());
            var20.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (Exception var19) {
                var19.printStackTrace();
            }

        }

        return replyString;
    }

    public static void httpGetBytes(@NotNull String url, @Nullable Map setProperty, int timeoutMs, @NotNull IReceivedMessageCallback callback) {
        Intrinsics.checkNotNullParameter(url, "url");
        Intrinsics.checkNotNullParameter(callback, "callback");
        httpCommandBytes(url, "GET", (String)null, setProperty, (String)null, timeoutMs, callback);
    }

    public static void httpPostBytes(@NotNull String url, @Nullable String postData, @Nullable Map setProperty, int timeoutMs, @NotNull IReceivedMessageCallback callback) {
        Intrinsics.checkNotNullParameter(url, "url");
        Intrinsics.checkNotNullParameter(callback, "callback");
        httpCommandBytes(url, "POST", postData, setProperty, (String)null, timeoutMs, callback);
    }

    private static void httpCommandBytes(String url, String requestMethod, String postData, Map setProperty, String contentType, int timeoutMs, IReceivedMessageCallback callback) {
        InputStream inputStream = (InputStream)null;
        int timeout = timeoutMs;
        if (timeoutMs < 0) {
            timeout = 10000;
        }

        try {
            URLConnection var10000 = (new URL(url)).openConnection();
            if (var10000 == null) {
                throw new NullPointerException("null cannot be cast to non-null type java.net.HttpURLConnection");
            }

            HttpURLConnection httpConn = (HttpURLConnection)var10000;
            httpConn.setRequestMethod(requestMethod);
            if (setProperty != null) {
                Iterator var12 = setProperty.keySet().iterator();

                while(var12.hasNext()) {
                    String key = (String)var12.next();
                    String value = (String)setProperty.get(key);
                    httpConn.setRequestProperty(key, value);
                }
            }

            if (contentType != null) {
                httpConn.setRequestProperty("Content-Type", contentType);
            }

            httpConn.setConnectTimeout(timeout);
            httpConn.setReadTimeout(timeout);
            if (postData == null) {
                httpConn.connect();
            } else {
                httpConn.setDoInput(true);
                httpConn.setDoOutput(true);
                OutputStream outputStream = httpConn.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                writer.write(postData);
                writer.flush();
                writer.close();
                outputStream.close();
            }

            int responseCode = httpConn.getResponseCode();
            if (responseCode == 200) {
                inputStream = httpConn.getInputStream();
            }

            if (inputStream == null) {
                Log.w(TAG, " http " + requestMethod + " Response Code Error: " + responseCode + ": " + url);
                callback.onErrorOccurred((Exception)(new NullPointerException()));
                callback.onCompleted();
                return;
            }

            try {
                int contentLength = httpConn.getContentLength();
                if (contentLength < 0) {
                    try {
                        Map headers = httpConn.getHeaderFields();
                        List valueList = (List)headers.get("X-FILE_SIZE");

                        try {
                            if (valueList != null) {
                                String var15 = getValue(valueList);
                                contentLength = Integer.parseInt(var15);
                            }
                        } catch (Exception var25) {
                            var25.printStackTrace();
                        }
                    } catch (Exception var26) {
                        var26.printStackTrace();
                    }
                }

                byte[] buffer = new byte[262144];
                int readBytes = 0;
                int readSize = inputStream.read(buffer, 0, 262144);

                while(true) {
                    if (readSize == -1) {
                        Log.v(TAG, "RECEIVED " + readBytes + " BYTES. (contentLength : " + contentLength + ')');
                        inputStream.close();
                        break;
                    }

                    callback.onReceive(readBytes, contentLength, readSize, buffer);
                    readBytes += readSize;
                    readSize = inputStream.read(buffer, 0, 262144);
                }
            } catch (Exception var27) {
                Log.w(TAG, "httpGet: exception: " + var27.getMessage());
                var27.printStackTrace();
                callback.onErrorOccurred(var27);
            } finally {
                try {
                    inputStream.close();
                } catch (Exception var24) {
                    var24.printStackTrace();
                }

            }
        } catch (Exception var29) {
            Log.w(TAG, "http " + requestMethod + " " + url + "  " + var29.getMessage());
            var29.printStackTrace();
            callback.onErrorOccurred(var29);
            callback.onCompleted();
            return;
        }

        callback.onCompleted();
    }

    private static final String getValue(List valueList) {
        boolean isFirst = true;
        StringBuilder values = new StringBuilder();
        Iterator var5 = valueList.iterator();

        while(var5.hasNext()) {
            String value = (String)var5.next();
            values.append(value);
            if (isFirst) {
                isFirst = false;
            } else {
                values.append(" ");
            }
        }

        String var10000 = values.toString();
        Intrinsics.checkNotNullExpressionValue(var10000, "values.toString()");
        return var10000;
    }

    @Nullable
    public static Bitmap httpGetBitmap(@NotNull String url, @Nullable Map setProperty, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommandBitmap(url, "GET", (String)null, setProperty, (String)null, timeoutMs);
    }

    @Nullable
    public static Bitmap httpPostBitmap(@NotNull String url, @Nullable String postData, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommandBitmap(url, "POST", postData, (Map)null, (String)null, timeoutMs);
    }

    private static Bitmap httpCommandBitmap(String url, String requestMethod, String postData, Map setProperty, String contentType, int timeoutMs) {
        InputStream inputStream = (InputStream)null;
        Bitmap bmp = (Bitmap)null;
        int timeout = timeoutMs;
        if (timeoutMs < 0) {
            timeout = 10000;
        }

        try {
            URLConnection var10000 = (new URL(url)).openConnection();
            if (var10000 == null) {
                throw new NullPointerException("null cannot be cast to non-null type java.net.HttpURLConnection");
            } else {
                HttpURLConnection httpConn = (HttpURLConnection)var10000;
                httpConn.setRequestMethod(requestMethod);
                if (setProperty != null) {
                    Iterator var12 = setProperty.keySet().iterator();

                    while(var12.hasNext()) {
                        String key = (String)var12.next();
                        String value = (String)setProperty.get(key);
                        httpConn.setRequestProperty(key, value);
                    }
                }

                if (contentType != null) {
                    httpConn.setRequestProperty("Content-Type", contentType);
                }

                httpConn.setConnectTimeout(timeout);
                httpConn.setReadTimeout(timeout);
                if (postData == null) {
                    httpConn.connect();
                } else {
                    httpConn.setDoInput(true);
                    httpConn.setDoOutput(true);
                    OutputStream outputStream = httpConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                    writer.write(postData);
                    writer.flush();
                    writer.close();
                    outputStream.close();
                }

                int responseCode = httpConn.getResponseCode();
                if (responseCode == 200) {
                    inputStream = httpConn.getInputStream();
                    if (inputStream != null) {
                        bmp = BitmapFactory.decodeStream(inputStream);
                    }
                }

                if (inputStream == null) {
                    Log.w(TAG, "http: (" + requestMethod + ") Response Code Error: " + responseCode + ": " + url);
                    return null;
                } else {
                    inputStream.close();
                    return bmp;
                }
            }
        } catch (Exception var14) {
            Log.w(TAG, "http: (" + requestMethod + ") " + url + "  " + var14.getMessage());
            var14.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static String httpPost(@NotNull String url, @Nullable String postData, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "POST", postData, (Map)null, (String)null, timeoutMs);
    }

    @Nullable
    public static String httpGetWithHeader(@NotNull String url, @Nullable Map headerMap, @Nullable String contentType, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "GET", (String)null, headerMap, contentType, timeoutMs);
    }

    @Nullable
    public static String httpPostWithHeader(@NotNull String url, @Nullable String postData, @Nullable Map headerMap, @Nullable String contentType, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "POST", postData, headerMap, contentType, timeoutMs);
    }

    @Nullable
    public static String httpPutWithHeader(@NotNull String url, @Nullable String putData, @Nullable Map headerMap, @Nullable String contentType, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "PUT", putData, headerMap, contentType, timeoutMs);
    }

    @Nullable
    public static String httpPut(@NotNull String url, @Nullable String postData, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "PUT", postData, (Map)null, (String)null, timeoutMs);
    }

    @Nullable
    public static String httpOptions(@NotNull String url, @Nullable String optionsData, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");
        return httpCommand(url, "OPTIONS", optionsData, (Map)null, (String)null, timeoutMs);
    }

    private static String httpCommand(String url, String requestMethod, String postData, Map setProperty, String contentType, int timeoutMs) {
        InputStream inputStream = (InputStream)null;
        int timeout = timeoutMs;
        if (timeoutMs < 0) {
            timeout = 10000;
        }

        try {
            URLConnection var10000 = (new URL(url)).openConnection();
            if (var10000 == null) {
                throw new NullPointerException("null cannot be cast to non-null type java.net.HttpURLConnection");
            }

            HttpURLConnection httpConn = (HttpURLConnection)var10000;
            httpConn.setRequestMethod(requestMethod);
            if (setProperty != null) {
                Iterator var11 = setProperty.keySet().iterator();

                while(var11.hasNext()) {
                    String key = (String)var11.next();
                    String value = (String)setProperty.get(key);
                    httpConn.setRequestProperty(key, value);
                }
            }

            if (contentType != null) {
                httpConn.setRequestProperty("Content-Type", contentType);
            }

            httpConn.setConnectTimeout(timeout);
            httpConn.setReadTimeout(timeout);
            if (postData == null) {
                httpConn.connect();
            } else {
                httpConn.setDoInput(true);
                httpConn.setDoOutput(true);
                OutputStream outputStream = httpConn.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                writer.write(postData);
                writer.flush();
                writer.close();
                outputStream.close();
            }

            int responseCode = httpConn.getResponseCode();
            if (responseCode == 200) {
                inputStream = httpConn.getInputStream();
            }

            if (inputStream == null) {
                Log.w(TAG, "http " + requestMethod + " : Response Code Error: " + responseCode + ": " + url);
                return "";
            }
        } catch (Exception var13) {
            Log.w(TAG, "http " + requestMethod + " : IOException: " + var13.getMessage());
            var13.printStackTrace();
            return "";
        }

        return readFromInputStream(inputStream);
    }

    private static String readFromInputStream(InputStream inputStream) {
        String replyString = "";
        if (inputStream == null) {
            return "";
        } else {
            try {
                StringBuilder responseBuf = new StringBuilder();
                BufferedReader reader = new BufferedReader((Reader)(new InputStreamReader(inputStream)));

                while(true) {
                    int newBuffer = reader.read();
                    if (newBuffer == -1) {
                        String var10000 = responseBuf.toString();
                        Intrinsics.checkNotNullExpressionValue(var10000, "responseBuf.toString()");
                        replyString = var10000;
                        reader.close();
                        break;
                    }

                    responseBuf.append((char)newBuffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return replyString;
        }
    }
    
    public interface IReceivedMessageCallback {
        void onCompleted();

        void onErrorOccurred(@Nullable Exception var1);

        void onReceive(int var1, int var2, int var3, @Nullable byte[] var4);
    }
    
}

