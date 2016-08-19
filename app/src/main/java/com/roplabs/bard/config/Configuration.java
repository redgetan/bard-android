package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "http://10.0.9.181:3000";
        } else {
            return "https://bard.co";
        }
    }
}
