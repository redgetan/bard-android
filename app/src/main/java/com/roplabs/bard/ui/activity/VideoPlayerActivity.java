package com.roplabs.bard.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.CrashReporter;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.HashMap;

public class VideoPlayerActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {

    private String videoLocation;
    private String repoToken;
    private String repoUrl;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        this.videoLocation = intent.getStringExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE);
        this.repoToken     = intent.getStringExtra(RepoListActivity.REPO_TOKEN_MESSAGE);
        this.repoUrl       = intent.getStringExtra(RepoListActivity.REPO_URL_MESSAGE);
        String repoTitle = intent.getStringExtra("title");

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);

        title.setText(repoTitle);

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

    public Intent getShareIntent() {
        Uri videoUri = Uri.fromFile(new File(videoLocation));
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }

    public void shareRepoInPlayer(View view) {
        startActivity(Intent.createChooser(getShareIntent(), "Share"));
    }

    public void onMoreBtnClick(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_repo_more);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copy_repo_link_item:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(this.repoUrl, this.repoUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ClientApp.getContext(), this.repoUrl + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.delete_repo_item:
//                delete(item);
                Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().deleteRepo(this.repoToken);
                final String targetRepoToken = this.repoToken;

                call.enqueue(new Callback<HashMap<String, String>>() {
                    @Override
                    public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                        if (!response.isSuccess()) {
                            displayError("Unable delete from remote server. " + response.body().get("error"));
                        } else {
                            HashMap<String, String> result = response.body();
                            Repo repo = Repo.forToken(targetRepoToken);

                            JSONObject properties = new JSONObject();
                            try {
                                properties.put("wordTags", repo.getWordList());
                                properties.put("characterToken", repo.getCharacterToken());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                CrashReporter.logException(e);
                            }
                            Analytics.track(ClientApp.getContext(), "deleteRepo", properties);

                            if(new File(repo.getFilePath()).delete()) {
                                repo.delete();
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                        displayError("Unable to delete video", t);
                    }
                });
                return true;
            default:
                return false;
        }
    }

    public void displayError(String message, Throwable t) {
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(t);
    }

    public void displayError(String message) {
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(new Throwable(message));
    }

}
