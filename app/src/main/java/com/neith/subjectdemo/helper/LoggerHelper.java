package com.neith.subjectdemo.helper;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoggerHelper {

    private static final String TAG = "AppLog";

    public static void log(String action, String message) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Log.d(TAG, "[" + time + "] [" + action + "] " + message);
    }

    public static void logError(String action, String message, Exception e) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Log.e(TAG, "[" + time + "] [" + action + "] " + message, e);
    }
}