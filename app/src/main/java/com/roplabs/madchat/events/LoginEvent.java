package com.roplabs.madchat.events;

import com.roplabs.madchat.models.User;

public class LoginEvent {
    public final User user;
    public final String error;

    public LoginEvent(User user, String error) {
        this.user = user;
        this.error = error;
    }
}
