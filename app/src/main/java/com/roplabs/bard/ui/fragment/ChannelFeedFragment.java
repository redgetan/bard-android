package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelFeedAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Post;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.ShareEditorActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.ui.widget.timeline.TimelineAdapter;
import com.roplabs.bard.util.*;
import im.ene.toro.Toro;
import im.ene.toro.ToroPlayer;
import im.ene.toro.ToroStrategy;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.roplabs.bard.util.Helper.SHARE_REPO_REQUEST_CODE;

public class ChannelFeedFragment extends Fragment implements
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener
{

    private List<Post> postList;
    private RecyclerView recyclerView;
    private EndlessRecyclerViewScrollListener scrollListener;
    private ChannelFeedAdapter adapter;
    private ProgressBar progressBar;
    private String channelToken;
    private ProgressBar channelFeedVideoProgress;
    private TextureView channelFeedVideo;
    private Surface channelFeedVideoSurface;
    private MediaPlayer mediaPlayer;
    private TextView debugView;
    private boolean isVideoReady;
    private String lastUrlPlayed = "";
    private Post currentPost;


    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;
    private ImageView likeChannelPostButton;
    private ImageView shareChannelPostButton;
    private LinearLayout channelFeedControls;
    private boolean isPostDownloadInProgress;

    private int MAX_PROGRESS_SHOWN_TIME = 10000;

    public static ChannelFeedFragment newInstance(String channelToken) {
        Bundle args = new Bundle();
        ChannelFeedFragment fragment = new ChannelFeedFragment();
        args.putString("channelToken", channelToken);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        channelToken = getArguments().getString("channelToken");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_channel_feed, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.channel_feed_list);
        progressBar = (ProgressBar) view.findViewById(R.id.channel_feed_progress_bar);
        channelFeedVideo = (TextureView) view.findViewById(R.id.channel_feed_video);
        channelFeedVideoProgress = (ProgressBar) view.findViewById(R.id.channel_feed_video_progress);
        debugView = (TextView) view.findViewById(R.id.channel_feed_video_debug);
        likeChannelPostButton = (ImageView) view.findViewById(R.id.like_channel_post_btn);
        shareChannelPostButton = (ImageView) view.findViewById(R.id.share_channel_post_btn);
        channelFeedControls = (LinearLayout) view.findViewById(R.id.channel_feed_control_container);
        channelFeedControls.setVisibility(View.GONE);

        this.postList = new ArrayList<Post>();

        initFeed();
        initEmptyState(view);
        getChannelFeedsNextPage(1);
        initVideoPlayer();

        return view;
    }



    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void initFeed() {
        final Context self = getActivity();

        adapter = new ChannelFeedAdapter(getActivity(), this.postList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);

        adapter.setOnItemClickListener(new ChannelFeedAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, final Post post) {
                if (isPostDownloadInProgress) return;

                if (currentPost != null) {
                    if (new File(currentPost.getCachedVideoFilePath()).exists()) {
                        playLocalVideo(currentPost.getCachedVideoFilePath());
                    }
                }

                isPostDownloadInProgress = true;
                Storage.cacheVideo(post.getCacheKey(), post.getRepoSourceUrl(), new Storage.OnCacheVideoListener() {
                    @Override
                    public void onCacheVideoSuccess(String filePath) {
                        isPostDownloadInProgress = false;
                        currentPost = post;
                        BardLogger.trace("video cached at " + filePath);
                        playLocalVideo(filePath);
                    }

                    @Override
                    public void onCacheVideoFailure() {
                        isPostDownloadInProgress = false;
                        Toast.makeText(getContext(),"Failed to download post", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // controls
        likeChannelPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        shareChannelPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save as Repo (store at last_shared_repo.mp4)
                if (currentPost != null) {
                    String repoToken = currentPost.getRepoToken();
                    Repo repo = Repo.forToken(repoToken);

                    if (repo != null ) {
                        if (new File(repo.getFilePath()).exists()) {
                        } else {
                            String repoFilePath = Storage.getLocalSavedFilePath();
                            Helper.copyFile(currentPost.getCachedVideoFilePath(),repoFilePath);

                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            repo.setFilePath(repoFilePath);
                            realm.commitTransaction();

                        }
                    } else {
                        String repoFilePath = Storage.getLocalSavedFilePath();
                        Helper.copyFile(currentPost.getCachedVideoFilePath(),repoFilePath);
                        String repoUrl = Configuration.bardAPIBaseURL() + "/r/" + repoToken;
                        repo = Repo.create(repoToken, repoUrl, "", "", repoFilePath, currentPost.getRepoWordList(), Calendar.getInstance().getTime());
                    }


                    Intent intent = new Intent(self, ShareEditorActivity.class);

                    intent.putExtra("wordTags", repo.getWordList());
                    intent.putExtra("sceneToken", "");
                    intent.putExtra("characterToken", "");
                    intent.putExtra("sceneName", "");
                    intent.putExtra("shareType", "repo");
                    intent.putExtra("repoToken", repo.getToken());
                    startActivityForResult(intent, SHARE_REPO_REQUEST_CODE);


                }

            }
        });


        // set decorator
//        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.scene_item_offset);
//        recyclerView.addItemDecoration(itemDecoration);

//        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
//            @Override
//            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
//                // Triggered only when new data needs to be appended to the list
//                // Add whatever code is needed to append new items to the bottom of the list
//                BardLogger.log("LOAD_MORE: " + page);
//                getChannelFeedsNextPage(page);
//            }
//        };
//
//        recyclerView.addOnScrollListener(scrollListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void getChannelFeedsNextPage(final int page) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("page", String.valueOf(page));

        // fetch remote

        Call<List<Post>> call = BardClient.getAuthenticatedBardService().getChannelPosts(channelToken, options);
        call.enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                List<Post> remotePostList = response.body();
                if (remotePostList == null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Request Failed");
                    emptyStateDescription.setText("Currently unable to fetch data from server. Try again later.");
                } else if (remotePostList.isEmpty() && page == 1) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Make your first Post");
                    emptyStateDescription.setText("No one has posted in this channel yet");
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    populateFeed(remotePostList);
                }

            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                emptyStateContainer.setVisibility(View.VISIBLE);
                emptyStateTitle.setText("Request Failed");
                emptyStateDescription.setText("Currently unable to fetch data from server. Try again later.");
            }
        });

    }



    private void populateFeed(List<Post> remotePostList) {
        int oldPosition = postList.size();
        int itemAdded = 0;
        for (Post remoteRepo : remotePostList) {
            if (!postList.contains(remoteRepo)) {
                postList.add(remoteRepo);
                itemAdded++;
            }
        }
        recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, itemAdded);

    }


    private void initVideoPlayer() {
        // video
        channelFeedVideo.setOpaque(false);
        channelFeedVideo.setSurfaceTextureListener(this);

        channelFeedVideo.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
            }

            @Override
            public void onSwipeRight() {
            }

            @Override
            public void onTouchUp() {
                if (!isVideoReady) return;

                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });


    }


    public void playLocalVideo(String sourceUrl) {
        debugView.setText("");
        if (!Helper.isConnectedToInternet()) {
            debugView.setText(R.string.no_network_connection);
            return;
        }

        if (lastUrlPlayed.equals(sourceUrl) && isVideoReady) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            return;
        }

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(sourceUrl);
            mediaPlayer.setSurface(channelFeedVideoSurface);

            channelFeedVideoProgress.setVisibility(View.VISIBLE);
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        channelFeedVideoProgress.setVisibility(View.GONE);
                    }
                },
            MAX_PROGRESS_SHOWN_TIME);


            mediaPlayer.prepareAsync();

            lastUrlPlayed = sourceUrl;
        } catch (IOException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }

    }





    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        channelFeedVideoSurface = new Surface(surface);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setSurface(channelFeedVideoSurface);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {


    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        BardLogger.trace("mediaplayer onPrepared");
        isVideoReady = true;
        channelFeedVideoProgress.setVisibility(View.GONE);
        channelFeedControls.setVisibility(View.VISIBLE);

        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

}
