package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.util.Helper;
import com.roplabs.bard.util.Storage;

import java.io.File;

import static com.roplabs.bard.util.Helper.SHARE_REPO_REQUEST_CODE;

public class EditorPreviewActivity extends BaseActivity {

    private String videoLocation;
    private VideoView videoView;
    private LinearLayout previewSaveButton;
    private ImageView previewSaveButtonIcon;
    private TextView previewSaveButtonLabel;
    private MediaPlayer mediaPlayer;
    private ImageView playBtn;

    private boolean isVideoReady = false;

    private String wordTagListString;
    private String sceneName;
    private String sceneToken;
    private Repo repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_preview);

        Intent intent = getIntent();
        sceneToken = intent.getStringExtra("sceneToken");
        sceneName = intent.getStringExtra("sceneName");
        wordTagListString = intent.getStringExtra("wordTags");

        videoView = (VideoView) findViewById(R.id.video_view);
        previewSaveButton = (LinearLayout) findViewById(R.id.preview_save_repo_button);
        previewSaveButtonIcon = (ImageView) findViewById(R.id.preview_save_repo_icon);
        previewSaveButtonLabel = (TextView) findViewById(R.id.preview_save_repo_label);
        playBtn = (ImageView) findViewById(R.id.editor_preview_play_button);

        initVideoPlayer();
        playLocalVideo(Storage.getMergedOutputFilePath());

    }

    private void playLocalVideo(String filePath) {
        playBtn.setVisibility(View.GONE);
        videoView.setVideoPath(filePath);
        videoView.start();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SHARE_REPO_REQUEST_CODE) {
            if (data != null) {
                String repoToken = data.getStringExtra("repoToken");
                if (repoToken != null) {
                    repo = Repo.forToken(repoToken);
                    markAsSaved();
                }
            }
        }
    }

    private void markAsSaved() {
        previewSaveButton.setEnabled(false);
        previewSaveButtonIcon.setVisibility(View.GONE);
        previewSaveButtonLabel.setText("Saved");
        previewSaveButtonLabel.setTextColor(ContextCompat.getColor(ClientApp.getContext(), R.color.md_green_300));
    }


    private void initVideoPlayer() {
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replayVideo();
            }
        });


        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playBtn.setVisibility(View.VISIBLE);
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
        playBtn.setVisibility(View.GONE);
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
//        getMenuInflater().inflate(R.menu.menu_video_player, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void backToEditor(View view) {
        finish();
    }

    public void openSharingInPreview(View view) {
        Intent intent = new Intent(this, ShareEditorActivity.class);

        intent.putExtra("wordTags", wordTagListString);
        intent.putExtra("sceneToken", sceneToken);
        intent.putExtra("sceneName", sceneName);
        if (repo != null) {
            intent.putExtra("repoToken", repo.getToken());
        }
        startActivityForResult(intent, SHARE_REPO_REQUEST_CODE);
    }

    public void saveRepoInPreview(View view) {
        if (this.repo != null) {
            // already saved (i.e. when generating online link)
            return;
        }

        Helper.saveLocalRepo(null, null, wordTagListString, sceneToken, sceneName, new Helper.OnRepoSaved() {
            @Override
            public void onSaved(Repo createdRepo) {
                repo = createdRepo;
                markAsSaved();
            }
        });

    }
}
