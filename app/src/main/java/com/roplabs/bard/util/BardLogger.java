package com.roplabs.bard.util;

import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.roplabs.bard.BuildConfig;

public class BardLogger {
    private static String TAG = "Bard";

    public static void log(String message) {
        // http://stackoverflow.com/a/23432225/803865
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.e(TAG, message);
        }
    }

    public static void trace(String message) {
        Crashlytics.log(Log.DEBUG, TAG, message);
    }
}
