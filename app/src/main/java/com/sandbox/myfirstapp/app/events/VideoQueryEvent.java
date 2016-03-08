package com.sandbox.myfirstapp.app.events;

public class VideoQueryEvent {
    public final String videoUrl;
    public final String error;

    public VideoQueryEvent(String videoUrl, String error) {
        this.videoUrl = videoUrl;
        this.error = error;
    }

}
