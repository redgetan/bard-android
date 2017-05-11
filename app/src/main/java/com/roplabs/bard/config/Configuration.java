package com.roplabs.bard.config;

import com.roplabs.bard.BuildConfig;

import java.util.Locale;

public class Configuration {
    public static String bardAPIBaseURL() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "http://10.0.9.18:3000";
        } else {
            return "https://bard.co";
        }
    }

    public static String bardLambdaBaseURL() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "http://10.0.9.18:9000";
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
        if (Locale.getDefault().getLanguage().equals(new Locale("ar").getLanguage())) {
            // arabic
            return "LRV1Bnd0yjY";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("nl").getLanguage())) {
            // dutch
            return "mqEHVR6lkKU";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("es").getLanguage())) {
            // spanish
            return "bsJAcWrNaSM";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("pt").getLanguage())) {
            // portuguese
            return "aET5aQaGJns";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("hi").getLanguage())) {
            // hindi
            return "KhRBeb9MidY";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("ja").getLanguage())) {
            // japanese
            return "NSTDEmCW2xU";
        } else if (Locale.getDefault().getLanguage().equals(new Locale("zh").getLanguage())) {
            // chinese
            return "C2iA0mFTX7s";
        } else {
            return "l9rkdM6ea4I";
        }

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

    public static String s3UserBucketCdnPath() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return "http://d3oyhzqd45hdnx.cloudfront.net";
        } else {
            return "https://videos.bard.co";
        }
    }
}
