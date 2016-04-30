package com.roplabs.madchat.events;

import android.text.TextUtils;
import com.roplabs.madchat.models.Segment;

import java.util.ArrayList;
import java.util.List;

public class VideoQueryEvent {
    public final List<Segment> segments;
    public final String error;

    public VideoQueryEvent(List<Segment> segments, String error) {
        this.segments = segments;
        this.error = error;
    }

}
