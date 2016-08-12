package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "http://192.168.1.3:3000";
        } else {
            return "https://bard.co";
        }
    }
}
