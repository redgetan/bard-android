package com.roplabs.bard.events;

import com.roplabs.bard.models.Segment;

import java.util.List;

public class VideoQueryEvent {
    public final List<Segment> segments;
    public final String error;
    public final boolean isPreview;

    public VideoQueryEvent(List<Segment> segments, String error, boolean isPreview) {
        this.segments = segments;
        this.error = error;
        this.isPreview = isPreview;
    }

}
