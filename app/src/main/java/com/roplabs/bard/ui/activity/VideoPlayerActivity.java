package com.roplabs.bard.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
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
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.CrashReporter;
import com.roplabs.bard.util.Helper;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.HashMap;

import static com.roplabs.bard.util.Helper.SHARE_REPO_REQUEST_CODE;

public class VideoPlayerActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {

    private String videoLocation;
    private String repoToken;
    private String repoUrl;
    private Repo repo;
    private Scene scene;
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
        videoView = (VideoView) findViewById(R.id.video_view);

        title.setText(repoTitle);
        this.repo = Repo.forToken(repoToken);
        String sceneToken = repo.getSceneToken();
        if (sceneToken != null) {
            this.scene = Scene.forToken(sceneToken);
        }

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
        File file = new File(videoLocation);
        Uri videoUri = FileProvider.getUriForFile(ClientApp.getContext(), getApplicationContext().getPackageName() + ".provider", file);
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }

    public void shareRepoInPlayer(View view) {
//        startActivity(Intent.createChooser(getShareIntent(), "Share"));
        Intent intent = new Intent(this, ShareEditorActivity.class);

        intent.putExtra("wordTags", this.repo.getWordList());
        intent.putExtra("sceneToken", this.repo.getSceneToken());

        if (this.scene != null) {
            intent.putExtra("sceneName", this.scene.getName());
        }

        if (repo != null) {
            intent.putExtra("repoToken", repo.getToken());
        }

        startActivityForResult(intent, SHARE_REPO_REQUEST_CODE);
    }

    public void onMoreBtnClick(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_repo_more);
        if (this.repo.getUrl() != null) {
            popup.getMenu().findItem(R.id.publish_repo_link_item).setTitle("Published");
            popup.getMenu().findItem(R.id.publish_repo_link_item).setEnabled(false);
        }
        popup.show();
    }

    public void deleteRepo(final Repo repo) {

        if (repo.getUrl() == null) {
            // local repo, delete locally only
            if(new File(repo.getFilePath()).delete()) {
                repo.delete();
                finish();
            }
            return;
        }

        // remote repo, delete both locally and on server
        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().deleteRepo(repo.getToken());

        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (!response.isSuccess()) {
                    // if no longer exist on server, just delete locally
                    displayError("Unable delete from remote server. ");
                } else {

                    if(new File(repo.getFilePath()).delete()) {
                        repo.delete();
                        finish();
                    }
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                // server error
                displayError("Unable to delete video", t);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SHARE_REPO_REQUEST_CODE) {
            if (data != null) {
                String repoToken = data.getStringExtra("repoToken");
                if (repoToken != null) {
                    repo = Repo.forToken(repoToken);
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Context self = this;
        switch (item.getItemId()) {
            case R.id.publish_repo_link_item:
                Helper.publishRepo(repo, this, new Helper.OnRepoPublished() {
                    @Override
                    public void onPublished(Repo publishedRepo) {
                        Toast.makeText(self,"Successfully published", Toast.LENGTH_LONG).show();
                    }
                });
                return true;
            case R.id.delete_repo_item:
                deleteRepo(repo);
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
