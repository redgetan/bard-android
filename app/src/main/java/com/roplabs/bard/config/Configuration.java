package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "https://bard.co";
        } else {
            return "https://bard.co";
        }
    }
}
