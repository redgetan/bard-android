package com.roplabs.madchat.events;

import android.text.TextUtils;
import com.roplabs.madchat.models.Segment;

import java.util.ArrayList;
import java.util.List;

public class VideoQueryEvent {
    public final String words;
    public final String error;

    public VideoQueryEvent(List<Segment> segments, String error) {
        List<String> wordList = new ArrayList<String>();
        for (Segment segment: segments) {
            wordList.add(segment.getWord());
        }
        this.words = TextUtils.join(",", wordList);
        this.error = error;
    }

}
