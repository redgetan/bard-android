package com.roplabs.bard.util;

import android.content.Context;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class CrashReporter {
    public static void init(Context context) {
        Fabric.with(context, new Crashlytics());
    }

    public static void logException(Throwable throwable) {
        Crashlytics.logException(throwable);
    }
}
