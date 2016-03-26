package com.sandbox.myfirstapp.app.models;

import android.content.Context;
import android.preference.PreferenceManager;

public class Setting {
    private static final String CURRENT_INDEX = "current_index";

    public static Long getCurrentIndex(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                        .getLong(CURRENT_INDEX, 0);
    }
    public static void setCurrentIndex(Context context, Long value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(CURRENT_INDEX, value)
                .apply();
    }
}
