package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Segment;
import com.roplabs.bard.models.VideoDownloader;
import com.roplabs.bard.util.Helper;
import com.roplabs.bard.util.Storage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.roplabs.bard.util.Helper.SHARE_REPO_REQUEST_CODE;

public class EditorPreviewActivity extends BaseActivity implements ExoPlayer.EventListener {

    private String videoLocation;
    private SimpleExoPlayerView videoView;
    private SimpleExoPlayer player;
    private LinearLayout previewSaveButton;
    private ImageView previewSaveButtonIcon;
    private TextView previewSaveButtonLabel;
    private MediaPlayer mediaPlayer;
    private ImageView playBtn;

    private boolean isVideoReady = false;

    private String wordTagListString;
    private String sceneName;
    private String sceneToken;
    private String characterToken;
    private String channelToken;
    private String repoToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_preview);

        Intent intent = getIntent();
        sceneToken = intent.getStringExtra("sceneToken");
        characterToken = intent.getStringExtra("characterToken");
        sceneName = intent.getStringExtra("sceneName");
        channelToken = intent.getStringExtra("channelToken");
        wordTagListString = intent.getStringExtra("wordTags");

        previewSaveButton = (LinearLayout) findViewById(R.id.preview_save_repo_button);
        previewSaveButtonIcon = (ImageView) findViewById(R.id.preview_save_repo_icon);
        previewSaveButtonLabel = (TextView) findViewById(R.id.preview_save_repo_label);
        playBtn = (ImageView) findViewById(R.id.editor_preview_play_button);

    }

    private void playLocalVideo() {
        playBtn.setVisibility(View.GONE);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "bard-player"));
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();


        List<String> cachedSegments = getCachedSegmentFiles();
        MediaSource[] mediaSources = new MediaSource[cachedSegments.size()];
        for (int i = 0; i < cachedSegments.size(); i++) {
            mediaSources[i] = new ExtractorMediaSource(Uri.fromFile(new File(cachedSegments.get(i))), dataSourceFactory, extractorsFactory, null, null);
        }
//        MediaSource source_a = new ExtractorMediaSource(Uri.parse("https://segments.bard.co/segments/Gvl2y34puhY/88951.mp4"), dataSourceFactory, extractorsFactory, null, null);
//        MediaSource source_b = new ExtractorMediaSource(Uri.parse("https://segments.bard.co/segments/Gvl2y34puhY/90972.mp4"), dataSourceFactory, extractorsFactory, null, null);
//        MediaSource source_c = new ExtractorMediaSource(Uri.parse("https://segments.bard.co/segments/Gvl2y34puhY/90047.mp4"), dataSourceFactory, extractorsFactory, null, null);
//        MediaSource source_c = new ExtractorMediaSource(Uri.parse("https://segments.bard.co/segments/u5mFI9spp10/V4OfkjCK0rU5.mp4"), dataSourceFactory, extractorsFactory, null, null);

        ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource(mediaSources);

//        ConcatenatingMediaSource concatenatedSource =
//                new ConcatenatingMediaSource(source_a, source_b, source_c);
// Prepare the player with the source.
        player.prepare(concatenatedSource);

    }

    @Override
    protected void onResume() {
        initVideoPlayer();

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseVideoPlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseVideoPlayer();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SHARE_REPO_REQUEST_CODE) {
            if (data != null) {
                repoToken = data.getStringExtra("repoToken");
                if (!repoToken.isEmpty()) {
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

    private void releaseVideoPlayer() {
        if (player != null) {
            player.removeListener(this);
            player.release();
            player = null;
        }
    }

    private void initVideoPlayer() {
        Handler mainHandler = new Handler();
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl(),
                null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF);
        player.setPlayWhenReady(true);
        videoView = (SimpleExoPlayerView) findViewById(R.id.video_view);
        videoView.setPlayer(player);
        player.addListener(this);
        playLocalVideo();




        // replay
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replayVideo();
            }
        });



//        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                playBtn.setVisibility(View.VISIBLE);
//            }
//        });
//
//        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                videoView.setBackgroundColor(Color.TRANSPARENT);
//                isVideoReady = true;
//                mediaPlayer = mp;
//            }
//        });

        videoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // http://stackoverflow.com/a/14163267
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (player.getPlaybackState() == ExoPlayer.STATE_READY ||
                        player.getPlaybackState() == ExoPlayer.STATE_ENDED   ) {
                        replayVideo();
                    }


                    return false;
                } else {
                    return true;
                }

            }
        });
    }

    public void replayVideo() {
        playLocalVideo();
//        playBtn.setVisibility(View.GONE);
//        player.seekTo(0);
//        player.setPlayWhenReady(true);
    }

    private List<String> getCachedSegmentFiles() {
        // we just need cached file location here (no need to know sceneToken)
        List<Segment> segments = Segment.buildFromWordTagList(Arrays.asList(wordTagListString.split(",")), "");

        // get only segments that exist
        List<String> validSegmentLocation = new ArrayList<String>();
        for (Segment segment : segments) {
            if (new File(segment.getFilePath()).exists()) {
                validSegmentLocation.add(segment.getFilePath());
            }
        }

        return validSegmentLocation;
    }

    private void deleteOldMergeVideo() {
        String outputFilePath = Storage.getMergedOutputFilePath();
        // delete old one before merging (since we rely on checking presence to see if merge is success or not)
        if ((new File(outputFilePath)).exists()) {
            new File(outputFilePath).delete();
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
        final Intent intent = new Intent(this, ShareEditorActivity.class);

        intent.putExtra("wordTags", wordTagListString);
        intent.putExtra("sceneToken", sceneToken);
        intent.putExtra("characterToken", characterToken);
        intent.putExtra("sceneName", sceneName);
        intent.putExtra("shareType", "repo");

        if (!repoToken.isEmpty()) {
            intent.putExtra("repoToken", repoToken);
            startActivityForResult(intent, SHARE_REPO_REQUEST_CODE);
        } else {
            mergeVideosAndSaveLocalRepo(new OnMergeSuccess() {
                @Override
                public void onMergeAndSaveComplete(String repoToken) {
                    intent.putExtra("repoToken", repoToken);
                    startActivityForResult(intent, SHARE_REPO_REQUEST_CODE);
                }
            });

        }
    }

    public void saveRepoInPreview(View view) {
        if (!repoToken.isEmpty()) {
            // already saved (i.e. when generating online link)
            return;
        }

        mergeVideosAndSaveLocalRepo(null);
    }

    public interface OnMergeSuccess {
        public void onMergeAndSaveComplete(String repoToken);
    }

    public void mergeVideosAndSaveLocalRepo(final OnMergeSuccess listener) {
        final Context self = this;

        Helper.mergeSegmentsRemotely(this, wordTagListString.replace(","," "), new Helper.OnMergeRemoteComplete() {
            @Override
            public void onMergeRemoteComplete(final String sourceUrl) {
                if (sourceUrl.isEmpty()) {
                    Toast.makeText(self, "Unable to process video", Toast.LENGTH_LONG).show();
                } else {
                    String[] sourceUrlTokens = sourceUrl.split("/");
                    String uuid = sourceUrlTokens[sourceUrlTokens.length - 1].replace("\\.mp4$","");
                    Helper.saveLocalRepo(null, null, uuid, wordTagListString, sceneToken, sceneName, characterToken, new Helper.OnRepoSaved() {
                        @Override
                        public void onSaved(final Repo createdRepo) {
                            repoToken = createdRepo.getToken();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    markAsSaved();
                                    if (listener != null) {
                                        listener.onMergeAndSaveComplete(repoToken);
                                    }
                                }
                            });

                        }
                    });
                }
            }
        });



    }



    /* repo can be either of 4 states
      1. not saved                                 (must save local + repos#create w/ channelToken)
      2. saved locally                             (repos#create w/ channelToken)
      3. saved locally + remotely                  (post_to_channel)
      4. saved locally + remotely + channel_posted (dont do anything)
    */
    public void postRepoToChannel(View view) {
        final EditorPreviewActivity self = this;
        Repo repo = Repo.forToken(repoToken);

        if (!channelToken.isEmpty()) {
            if (repo == null) {
                // 1. not saved                                 (must save local + repos#create w/ channelToken)
                mergeVideosAndSaveLocalRepo(new OnMergeSuccess() {
                    @Override
                    public void onMergeAndSaveComplete(String repoToken) {
                        Repo repo = Repo.forToken(repoToken);
                        Helper.saveRemoteRepo(repo, repo.getUUID(), channelToken, new Helper.OnRepoPublished() {
                            @Override
                            public void onPublished(Repo publishedRepo) {
                                Intent intent = new Intent();
                                intent.putExtra("backToChannel", true);
                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        });
                    }
                });

            } else if (!repo.getIsPublished()) {
                // 2. saved locally                             (repos#create w/ channelToken)

                Helper.saveRemoteRepo(repo, repo.getUUID(), channelToken, new Helper.OnRepoPublished() {
                    @Override
                    public void onPublished(Repo publishedRepo) {
                        Intent intent = new Intent();
                        intent.putExtra("backToChannel", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            } else if (repo.getIsPublished()){
                // 3. saved locally + remotely                  (post_to_channel)

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("channel_token", channelToken);
                Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postRepoToChannel(repo.getToken(), map);
                call.enqueue(new Callback<HashMap<String, String>>() {
                    @Override
                    public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                        Intent intent = new Intent();
                        intent.putExtra("backToChannel", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                        Toast.makeText(self,"Unable to post to channel", Toast.LENGTH_LONG).show();
                    }
                });
            }

        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
           playBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }
}
