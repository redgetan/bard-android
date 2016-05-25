package com.roplabs.bard.events;

import com.roplabs.bard.models.Index;

public class IndexSelectEvent {
    public final Index index;
    public final String error;

    public IndexSelectEvent(Index index, String error) {
        this.index = index;
        this.error = error;
    }
}
