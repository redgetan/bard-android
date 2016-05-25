package com.roplabs.bard.events;

import com.roplabs.bard.models.Segment;

import java.util.List;

public class VideoQueryEvent {
    public final List<Segment> segments;
    public final String error;

    public VideoQueryEvent(List<Segment> segments, String error) {
        this.segments = segments;
        this.error = error;
    }

}
