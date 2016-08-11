package com.roplabs.bard.util;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.models.Setting;
import org.json.JSONObject;

import java.util.Date;

public class Analytics {
    private static MixpanelAPI mixpanel;

    public static void identify() {
        mixpanel.getPeople().identify(Setting.getUsername(ClientApp.getContext()));
        mixpanel.getPeople().set("$name", Setting.getUsername(ClientApp.getContext()));
        mixpanel.getPeople().set("$email", Setting.getEmail(ClientApp.getContext()));

    }

    public static void identify(Date createdAt) {
        identify();
        mixpanel.getPeople().set("$created", createdAt);
    }

    public static MixpanelAPI getMixpanelInstance() {
        if (mixpanel == null ) {
            mixpanel = MixpanelAPI.getInstance(ClientApp.getContext(), "8492ff36260ca3f573d6eed22bb94ece");
        }

        return mixpanel;
    }

    public static void timeEvent(String event) {
        getMixpanelInstance().timeEvent(event);
    }

    public static void track(String event) {
        getMixpanelInstance().track(event);
    }

    public static void track(String event, JSONObject properties) {
        getMixpanelInstance().track(event, properties);
    }
}
