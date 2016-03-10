package com.sandbox.myfirstapp.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.VideoView;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.Repo;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Intent intent = getIntent();
        String videoLocation = intent.getStringExtra(UserRepoListActivity.VIDEO_LOCATION_MESSAGE);

        VideoView videoView = (VideoView) findViewById(R.id.video_view);
        videoView.setVideoPath(videoLocation);
        videoView.requestFocus();
        videoView.start();

    }
}
