package com.roplabs.madchat.models;

import android.content.Context;
import android.preference.PreferenceManager;

public class Setting {
    private static final String CURRENT_INDEX_TOKEN = "current_index_token";
    private static final String AUTHENTICATION_TOKEN = "authentication_token";
    private static final String USERNAME = "username";
    private static final String EMAIL = "email";

    public static String getCurrentIndexToken(Context context) {
        return get(context, CURRENT_INDEX_TOKEN);
    }

    public static void setCurrentIndexToken(Context context, String token) {
        set(context,CURRENT_INDEX_TOKEN,token);
    }

    public static String getAuthenticationToken(Context context) {
        return get(context, AUTHENTICATION_TOKEN);
    }

    public static void setAuthenticationToken(Context context, String token) {
        set(context,AUTHENTICATION_TOKEN,token);
    }

    public static String getUsername(Context context) {
        return get(context, USERNAME);
    }

    public static void setUsername(Context context, String token) {
        set(context,USERNAME,token);
    }

    public static String getEmail(Context context) {
        return get(context, EMAIL);
    }

    public static void setEmail(Context context, String token) {
        set(context,EMAIL,token);
    }

    public static String get(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, "");
    }
    public static void set(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply();
    }
}
