package com.roplabs.bard.events;

import com.roplabs.bard.models.WordTag;

public class FetchWordClipEvent {
    public final WordTag wordTag;

    public FetchWordClipEvent(WordTag wordTag) {
        this.wordTag = wordTag;
    }
}
