package com.roplabs.madchat.events;

public class SignUpEvent {
    public final String authenticationToken;
    public final String error;

    public SignUpEvent(String authenticationToken, String error) {
        this.authenticationToken = authenticationToken;
        this.error = error;
    }
}
