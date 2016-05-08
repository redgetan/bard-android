package com.roplabs.madchat.events;

import com.roplabs.madchat.models.Index;

public class IndexSelectEvent {
    public final Index index;
    public final String error;

    public IndexSelectEvent(Index index, String error) {
        this.index = index;
        this.error = error;
    }
}
