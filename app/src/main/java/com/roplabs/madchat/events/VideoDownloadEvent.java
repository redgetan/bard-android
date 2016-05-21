package com.roplabs.madchat.events;

import com.roplabs.madchat.models.Segment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoDownloadEvent {
    public final String videoPath;
    public final List<Segment> segments;
    public final String error;

    public VideoDownloadEvent(HashMap<String, String> result) {
        this.videoPath = result.get("videoPath");
        this.segments = null;
        this.error = result.get("error");
    }

    public VideoDownloadEvent(List<Segment> segments) {
        this.videoPath = null;
        this.segments = segments;
        this.error = null;
    }
}

