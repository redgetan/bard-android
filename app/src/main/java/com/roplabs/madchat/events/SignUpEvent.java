package com.roplabs.madchat.events;

import com.roplabs.madchat.models.User;

public class SignUpEvent {
    public final User user;
    public final String error;

    public SignUpEvent(User user, String error) {
        this.user = user;
        this.error = error;
    }
}
