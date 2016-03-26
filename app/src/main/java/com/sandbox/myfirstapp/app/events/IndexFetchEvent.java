package com.sandbox.myfirstapp.app.events;

import com.sandbox.myfirstapp.app.models.Index;

import java.util.List;

public class IndexFetchEvent {
    public final List<Index> indexList;
    public final String error;

    public IndexFetchEvent(List<Index> indexList, String error) {
        this.indexList = indexList;
        this.error = error;
    }

}
