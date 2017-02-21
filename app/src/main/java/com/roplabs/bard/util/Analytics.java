package com.roplabs.bard.util;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Setting;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

public class Analytics {
    private static MixpanelAPI mixpanel;
    private static FirebaseAnalytics mFirebaseAnalytics;


    public static void identify(Context context) {
        getMixpanelInstance(context).identify(Setting.getUsername(context));
        getMixpanelInstance(context).getPeople().identify(Setting.getUsername(context));
        getMixpanelInstance(context).getPeople().set("$name", Setting.getUsername(context));
        getMixpanelInstance(context).getPeople().set("$email", Setting.getEmail(context));

    }

    public static void identify(Context context, Date createdAt) {
        identify(context);
        getMixpanelInstance(context).getPeople().set("$created", createdAt);
    }

    public static MixpanelAPI getMixpanelInstance(Context context) {
        if (mixpanel == null ) {
            mixpanel = MixpanelAPI.getInstance(context, Configuration.mixpanelAPIKey());
        }

        return mixpanel;
    }

    public static FirebaseAnalytics getFirebaseInstance(Context context) {
        if (mFirebaseAnalytics == null ) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }

        return mFirebaseAnalytics ;
    }


    public static void timeEvent(Context context, String event) {
        getMixpanelInstance(context).timeEvent(event);
    }

    public static void track(Context context, String event) {
        getMixpanelInstance(context).track(event);
    }

    public static void track(Context context, String event, JSONObject properties) {
        getMixpanelInstance(context).track(event, properties);
    }

    public static void track(Context context, String event, Bundle properties) {
        getFirebaseInstance(context).logEvent(event, properties);
    }

    public static void sendQueuedEvents(Context context) {
        getMixpanelInstance(context).flush();
    }
}
