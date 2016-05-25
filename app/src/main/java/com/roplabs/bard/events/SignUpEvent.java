package com.roplabs.bard.events;

import com.roplabs.bard.models.User;

public class SignUpEvent {
    public final User user;
    public final String error;

    public SignUpEvent(User user, String error) {
        this.user = user;
        this.error = error;
    }
}
