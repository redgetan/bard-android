package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "http://10.0.9.18:3000";
        } else {
            return "https://bard.co";
        }
    }

    public static String mixpanelAPIKey() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "8492ff36260ca3f573d6eed22bb94ece";
        } else {
            return "13ba4a4181b83c3569abf4336ef275d4";
        }
    }

    public static String cognitoIdentityPool() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "us-west-2:7bd4263f-57f2-4d08-9855-7672299d73d4";
        } else {
            return "us-west-2:a42a156a-30f6-4fb7-a2ea-78599fa4d180";
        }
    }

    public static String segmentsCdnPath() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "https://s3-us-west-2.amazonaws.com/roplabs-mad-staging";
        } else {
            return "https://segments.bard.co";
        }
    }

    public static String s3UserBucket() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return "roplabs-bard-users-staging";
        } else {
            return "roplabs-bard-users";
        }
    }
}
