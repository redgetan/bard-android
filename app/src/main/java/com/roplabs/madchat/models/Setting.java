package com.roplabs.madchat.models;

import android.content.Context;
import android.preference.PreferenceManager;

public class Setting {
    private static final String CURRENT_INDEX_TOKEN = "current_index_token";

    public static String getCurrentIndexToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(CURRENT_INDEX_TOKEN, "");
    }
    public static void setCurrentIndexToken(Context context, String token) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(CURRENT_INDEX_TOKEN, token)
                .apply();
    }
}
