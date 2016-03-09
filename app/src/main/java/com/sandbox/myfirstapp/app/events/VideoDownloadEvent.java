package com.sandbox.myfirstapp.app.events;

import java.util.HashMap;

public class VideoDownloadEvent {
    public final String videoPath;
    public final String error;

    public VideoDownloadEvent(HashMap<String, String> result) {
        this.videoPath = result.get("videoPath");
        this.error = result.get("error");
    }
}

