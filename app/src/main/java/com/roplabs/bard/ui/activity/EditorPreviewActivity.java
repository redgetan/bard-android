package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;
import com.roplabs.bard.R;

import java.io.File;

public class EditorPreviewActivity extends BaseActivity {

    private String videoLocation;
    private String repoToken;
    private String repoUrl;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_preview);

        Intent intent = getIntent();
        this.videoLocation = intent.getStringExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE);
        this.repoToken     = intent.getStringExtra(RepoListActivity.REPO_TOKEN_MESSAGE);
        this.repoUrl       = intent.getStringExtra(RepoListActivity.REPO_URL_MESSAGE);

        videoView = (VideoView) findViewById(R.id.video_view);
        initVideoPlayer();
        playLocalVideo(this.videoLocation);

    }

    private void playLocalVideo(String filePath) {
        videoView.setVideoPath(filePath);
        videoView.start();
    }


    private void initVideoPlayer() {
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
            }
        });

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

                    replayVideo();

                    return false;
                } else {
                    return true;
                }

            }
        });
    }

    public void replayVideo() {
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
//        getMenuInflater().inflate(R.menu.menu_video_player, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
