package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import com.roplabs.bard.R;

import java.io.File;

public class VideoPlayerActivity extends BaseActivity {

    private String videoLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        this.videoLocation = intent.getStringExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE);
        String repoTitle = intent.getStringExtra("title");

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(repoTitle);

        VideoView videoView = (VideoView) findViewById(R.id.video_view);
        videoView.setMediaController(new MediaController(this));
        videoView.setVideoPath(this.videoLocation);
        videoView.requestFocus();
        videoView.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.menu_video_player, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                startActivity(getShareIntent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Intent getShareIntent() {
        Uri videoUri = Uri.fromFile(new File(videoLocation));
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }

}
