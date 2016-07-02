package com.roplabs.bard.events;

import com.roplabs.bard.models.WordTag;

public class PreviewWordEvent {
    public final WordTag wordTag;
    public PreviewWordEvent(WordTag wordTag) {
        this.wordTag = wordTag;
    }
}
