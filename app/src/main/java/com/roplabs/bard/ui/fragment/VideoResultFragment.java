package com.roplabs.bard.ui.fragment;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;
import com.roplabs.bard.R;
import com.roplabs.bard.models.WordTag;
import com.roplabs.bard.ui.activity.BardEditorActivity;

import java.util.ArrayList;
import java.util.List;

public class VideoResultFragment extends Fragment {
    // Store instance variables
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_result, container, false);

        videoView = (VideoView) view.findViewById(R.id.video_view);

        initVideoPlayer();

        return view;
    }

    public VideoView getVideoView() {
        return videoView;
    }

    private void initVideoPlayer() {
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.setBackgroundColor(Color.TRANSPARENT);
                isVideoReady = true;
                mediaPlayer = mp;
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // http://stackoverflow.com/a/14163267
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!isVideoReady) return false;

                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();

                    return false;
                } else {
                    return true;
                }

            }
        });
    }


    public void playLocalVideo(String filePath) {
        videoView.setVideoPath(filePath);
        videoView.start();
    }

}
