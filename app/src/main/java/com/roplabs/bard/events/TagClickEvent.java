package com.roplabs.bard.events;

import com.roplabs.bard.models.WordTag;

public class TagClickEvent {
    public final WordTag wordTag;

    public TagClickEvent(WordTag wordTag) {
        this.wordTag = wordTag;
    }
}
