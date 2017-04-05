package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "http://100.65.102.2:3000";
        } else {
            return "https://bard.co";
        }
    }

    public static String bardLambdaBaseURL() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "http://100.65.102.2:9000";
        } else {
            return "https://api.bard.co";
        }
    }

    public static String cognitoIdentityPool() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "us-west-2:7bd4263f-57f2-4d08-9855-7672299d73d4";
        } else {
            return "us-west-2:a42a156a-30f6-4fb7-a2ea-78599fa4d180";
        }
    }

    public static String mainChannelToken() {
        return "l9rkdM6ea4I";
    }

    public static String segmentsCdnPath() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "https://s3-us-west-2.amazonaws.com/roplabs-mad-staging";
        } else {
            return "https://segments.bard.co";
        }
    }

    public static String s3UserBucket() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "roplabs-bard-users-staging";
        } else {
            return "roplabs-bard-users";
        }
    }
}
