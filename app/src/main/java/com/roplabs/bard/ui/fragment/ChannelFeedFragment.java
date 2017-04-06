package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
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
import com.roplabs.bard.models.Like;
import com.roplabs.bard.models.Post;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.BardEditorActivity;
import com.roplabs.bard.ui.activity.ShareEditorActivity;
import com.roplabs.bard.ui.widget.CustomDialog;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static android.app.Activity.RESULT_OK;
import static com.roplabs.bard.util.Helper.BARD_EDITOR_REQUEST_CODE;
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


    private CustomDialog loginDialog;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;
    private LinearLayout likeChannelPostButton;
    private FloatingActionButton createChannelPostButton;
    private LinearLayout shareChannelPostButton;
    private LinearLayout reuseChannelPostButton;
    private LinearLayout channelFeedControls;
    private boolean isPostDownloadInProgress;
    private OnChannelFeedListener parentListener;
    private boolean isFirstItemLoading = false;
    private ImageView playBtn;

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
        likeChannelPostButton = (LinearLayout) view.findViewById(R.id.like_channel_post_btn);
        createChannelPostButton = (FloatingActionButton) view.findViewById(R.id.create_channel_post_btn);
        shareChannelPostButton = (LinearLayout) view.findViewById(R.id.share_channel_post_btn);
        reuseChannelPostButton = (LinearLayout) view.findViewById(R.id.reuse_channel_post_btn);
        channelFeedControls = (LinearLayout) view.findViewById(R.id.channel_feed_control_container);
        channelFeedControls.setVisibility(View.GONE);
        playBtn = (ImageView) view.findViewById(R.id.channel_preview_play_button);

        this.postList = new ArrayList<Post>();

        initFeed();
        initEmptyState(view);
        initVideoPlayer();

        return view;
    }



    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
        emptyStateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyStateContainer.setVisibility(View.GONE);
                getChannelFeedsNextPage(1);
            }
        });
    }

    private void showFeedVideoProgress() {
        BardLogger.trace("showing video download progress");
        channelFeedVideoProgress.setVisibility(View.VISIBLE);
        new android.os.Handler().postDelayed(new Runnable() {
            public void run() {
                channelFeedVideoProgress.setVisibility(View.GONE);
            }
        }, MAX_PROGRESS_SHOWN_TIME);
    }

    private void hideFeedVideoProgress() {
        BardLogger.trace("hide video download progress");
        channelFeedVideoProgress.setVisibility(View.GONE);
    }

    private void playPost(Post post) {
        if (isPostDownloadInProgress) return;

        if (currentPost != post) {
            // post changed
            currentPost = post;
            setLikeButtonState();
        }

        showFeedVideoProgress();

        if (new File(currentPost.getCachedVideoFilePath()).exists()) {
            BardLogger.trace("post video already exist in cache, using it instead");
            playLocalVideo(currentPost.getCachedVideoFilePath());
            return;
        }

        if (!Helper.isConnectedToInternet()) {
            debugView.setText(R.string.no_network_connection);
            return;
        }

        isPostDownloadInProgress = true;


        Storage.cacheVideo(post.getCacheKey(), post.getRepoSourceUrl(), new Storage.OnCacheVideoListener() {
            @Override
            public void onCacheVideoSuccess(String filePath) {
                isPostDownloadInProgress = false;
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

    public interface OnChannelFeedListener  {
        // This can be any number of events to be sent to the activity
        public void onCreatePostClicked();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnChannelFeedListener) {
            parentListener = (OnChannelFeedListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SceneSelectFragment.OnSceneListener");
        }
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
                isFirstItemLoading = false;
                playPost(post);
            }
        });

        // controls

        createChannelPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentListener.onCreatePostClicked();
            }
        });

        likeChannelPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRepoLike();
            }
        });

        reuseChannelPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!currentPost.getSceneToken().isEmpty()) {
                    Intent intent = new Intent(getActivity(), BardEditorActivity.class);
                    intent.putExtra("channelToken", channelToken);
                    intent.putExtra("characterToken", "");
                    intent.putExtra("sceneToken", currentPost.getSceneToken());
                    startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
                } else if (!currentPost.getPackToken().isEmpty()) {
                    Intent intent = new Intent(getActivity(), BardEditorActivity.class);
                    intent.putExtra("channelToken", channelToken);
                    intent.putExtra("characterToken", currentPost.getPackToken());
                    intent.putExtra("sceneToken", "");
                    startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
                }

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
                        repo = Repo.create(repoToken, repoUrl, "", currentPost.getPackToken(), currentPost.getSceneToken(), repoFilePath, currentPost.getRepoWordList(), Calendar.getInstance().getTime());
                    }


                    Intent intent = new Intent(self, ShareEditorActivity.class);

                    intent.putExtra("wordTags", repo.getWordList());
                    intent.putExtra("sceneToken", currentPost.getSceneToken());
                    intent.putExtra("characterToken", currentPost.getPackToken());
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

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                BardLogger.log("LOAD_MORE: " + page);
                getChannelFeedsNextPage(page);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void getChannelFeedsNextPage(final int page) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("page", String.valueOf(page));

        // fetch remote

        progressBar.setVisibility(View.VISIBLE);

        Call<List<Post>> call = BardClient.getAuthenticatedBardService().getChannelPosts(channelToken, options);
        call.enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                progressBar.setVisibility(View.GONE);
                List<Post> remotePostList = response.body();
                if (remotePostList == null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Request Failed");
                    emptyStateDescription.setText("Tap to refresh.");
                } else if (remotePostList.isEmpty() && page == 1) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Make your first Post");
                    emptyStateDescription.setText("No one has posted in this channel yet");
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    populateFeed(remotePostList);

                    // play first item if first page loaded
                    if (!postList.isEmpty() && page == 1) {
                        int firstItemPosition = 0;
                        isFirstItemLoading = true;
                        playPost(postList.get(0));
                        ((ChannelFeedAdapter) recyclerView.getAdapter()).setSelected(firstItemPosition);
                    }
                }

            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyStateContainer.setVisibility(View.VISIBLE);
                emptyStateTitle.setText("Request Failed");
                emptyStateDescription.setText("Tap to refresh.");
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void refreshFeed() {
        postList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        getChannelFeedsNextPage(1);
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
        playBtn.setVisibility(View.GONE);
        debugView.setText("");
        if (lastUrlPlayed.equals(sourceUrl) && isVideoReady) {
            BardLogger.trace("sourceUrl: " + sourceUrl + " already loaded in mediaplayer. replaying it..");
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            return;
        }

        try {
            BardLogger.trace("source/prepare mediaPlayer: " + sourceUrl);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(sourceUrl);
            mediaPlayer.setSurface(channelFeedVideoSurface);

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

        isVideoReady = false;

        // only here do we call get feed as mediaplayer already initialized
        refreshFeed();
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
        playBtn.setVisibility(View.VISIBLE);
        BardLogger.trace("mediaplayer onPrepared. playing it");
        isVideoReady = true;
        hideFeedVideoProgress();
        channelFeedControls.setVisibility(View.VISIBLE);

        // if its first video, dont play it (just load it)
        if (isFirstItemLoading) {
            isFirstItemLoading = false;
        } else {
            mp.start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playBtn.setVisibility(View.VISIBLE);
    }

    public void toggleRepoLike() {
        Like like = Like.forRepoTokenAndUsername(currentPost.getRepoToken(), Setting.getUsername(getActivity()));
        if (like == null) {
            // create
            doLikeRepo(currentPost.getRepoToken());
        } else {
            // delete
            doUnlikeRepo(like, currentPost.getRepoToken());
        }
    }

    private void doLikeRepo(final String repoToken) {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(getActivity())) {
            loginDialog = new CustomDialog(getActivity(), "You must login to like user posts");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        likeChannelPostButton.setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().likeRepo(repoToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                likeChannelPostButton.setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                Like.create(realm, repoToken, Setting.getUsername(ClientApp.getContext()));
                realm.commitTransaction();

                setLikeButtonState();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                likeChannelPostButton.setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    private void doUnlikeRepo(final Like like, String repoToken) {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(getActivity())) {
            loginDialog = new CustomDialog(getActivity(), "You must login to like user posts");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        likeChannelPostButton.setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().unlikeRepo(repoToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                likeChannelPostButton.setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                like.deleteFromRealm();
                realm.commitTransaction();

                setLikeButtonState();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                likeChannelPostButton.setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    private void setLikeButtonState() {
        Like favorite = Like.forRepoTokenAndUsername(currentPost.getRepoToken(), Setting.getUsername(getActivity()));
        TextView likeButtonLabel = (TextView) likeChannelPostButton.findViewById(R.id.like_repo_label);
        ImageView likeButtonIcon = (ImageView) likeChannelPostButton.findViewById(R.id.like_repo_icon);
        if (favorite != null) {
            likeButtonLabel.setText("Liked");
            likeButtonIcon.setImageResource(R.drawable.ic_favorite_white_18dp);
        } else {
            likeButtonLabel.setText("Like");
            likeButtonIcon.setImageResource(R.drawable.ic_favorite_border_white_18dp);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CustomDialog.LOGIN_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(getContext(), "Login successful", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.SIGNUP_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(getContext(), "Account successfully created", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == SHARE_REPO_REQUEST_CODE) {
        }
    }

}
