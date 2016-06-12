package com.roplabs.bard.events;

public class InvalidWordEvent {
    public final String text;

    public InvalidWordEvent(String text) {
        this.text = text;
    }
}
