package com.sandbox.myfirstapp.app.events;

public class VideoQueryEvent {
    public final String videoUrl;

    public VideoQueryEvent(String videoUrl) {
        this.videoUrl = videoUrl;
    }

}
