package com.roplabs.bard.util;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Setting;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

public class Analytics {
    private static FirebaseAnalytics mFirebaseAnalytics;
    private static long startTime;
    private static long endTime;


    public static void identify(Context context) {

    }

    public static void identify(Context context, Date createdAt) {
        identify(context);
    }

    public static FirebaseAnalytics getFirebaseInstance(Context context) {
        if (mFirebaseAnalytics == null ) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }

        return mFirebaseAnalytics ;
    }


    public static void timeEvent(Context context, String event) {
        startTime = System.currentTimeMillis();

    }

    public static void track(Context context, String event) {

    }

    public static void track(Context context, String event, Bundle properties) {
        endTime = System.currentTimeMillis();
        if (event.equals("generateBardVideo")) {
            long duration = (endTime - startTime) / 1000;
            BardLogger.log("generateBardVideo took: " + duration + " seconds");
            properties.putString("duration", String.valueOf(duration));
        }
        getFirebaseInstance(context).logEvent(event, properties);
    }

}
