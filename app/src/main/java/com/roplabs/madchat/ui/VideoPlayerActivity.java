package com.roplabs.madchat.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.VideoView;
import com.roplabs.madchat.R;

import java.io.File;

public class VideoPlayerActivity extends BaseActivity {

    private String videoLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        this.videoLocation = intent.getStringExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE);

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

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // http://stackoverflow.com/a/21630571/
        ShareActionProvider mShareActionProvider = new ShareActionProvider(this);
        MenuItemCompat.setActionProvider(item, mShareActionProvider);
        mShareActionProvider.setShareIntent(getShareIntent());

        // Return true to display menu
        return true;
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
