package com.roplabs.bard.ui.widget.timeline;

import android.support.annotation.DrawableRes;
import com.roplabs.bard.R;

public class FbUser {

    private final int profileImageUrl;
    private final String userName;
    private final String userDescription;
    private final String userUrl;

    public FbUser() {
        profileImageUrl = R.drawable.ic_person_add_black_24dp;
        userName = "Toro Creator";
        userDescription = "nameless";
        userUrl = "https://github.com/";
    }

    @DrawableRes
    public int getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public String getUserUrl() {
        return userUrl;
    }
}

