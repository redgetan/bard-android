package com.roplabs.bard.events;

import com.roplabs.bard.models.WordTag;

public class ReplaceWordEvent {
    public final WordTag wordTag;

    public ReplaceWordEvent(WordTag wordTag) {
        this.wordTag = wordTag;
    }
}
