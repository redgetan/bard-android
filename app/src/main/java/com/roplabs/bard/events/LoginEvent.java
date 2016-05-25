package com.roplabs.bard.events;

import com.roplabs.bard.models.User;

public class LoginEvent {
    public final User user;
    public final String error;

    public LoginEvent(User user, String error) {
        this.user = user;
        this.error = error;
    }
}
