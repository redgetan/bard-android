package com.sandbox.myfirstapp.app.events;

public class VideoQueryEvent {
    public final String token;
    public final String videoUrl;
    public final String wordList;
    public final String error;

    public VideoQueryEvent(String token, String videoUrl, String wordList, String error) {
        this.token = token;
        this.videoUrl = videoUrl;
        this.wordList = wordList;
        this.error = error;
    }

}
