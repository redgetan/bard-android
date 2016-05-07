package com.roplabs.madchat.events;

public class LoginEvent {
    public final String authenticationToken;
    public final String error;

    public LoginEvent(String authenticationToken, String error) {
        this.authenticationToken = authenticationToken;
        this.error = error;
    }
}
