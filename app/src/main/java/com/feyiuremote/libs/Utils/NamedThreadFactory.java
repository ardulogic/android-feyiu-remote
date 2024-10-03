package com.feyiuremote.libs.Utils;

import android.util.Log;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
    private final String threadNamePrefix;

    public NamedThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(threadNamePrefix + "-" + thread.getId());
        Log.d("Thread Factory", "Allocating:" + thread.getName());
        return thread;
    }
}