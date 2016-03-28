package com.roplabs.madchat.events;

import com.roplabs.madchat.models.Index;

import java.util.List;

public class IndexFetchEvent {
    public final List<Index> indexList;
    public final String error;

    public IndexFetchEvent(List<Index> indexList, String error) {
        this.indexList = indexList;
        this.error = error;
    }

}
