package com.sandbox.myfirstapp.app.events;

public class VideoQueryEvent {
    public final String videoUrl;
    public final String wordList;
    public final String error;

    public VideoQueryEvent(String videoUrl, String wordList, String error) {
        this.videoUrl = videoUrl;
        this.wordList = wordList;
        this.error = error;
    }

}
