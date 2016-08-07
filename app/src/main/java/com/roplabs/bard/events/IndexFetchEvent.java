package com.roplabs.bard.events;

import com.roplabs.bard.models.Character;

import java.util.List;

public class IndexFetchEvent {
    public final List<Character> characterList;
    public final String error;

    public IndexFetchEvent(List<Character> characterList, String error) {
        this.characterList = characterList;
        this.error = error;
    }

}
