package com.roplabs.bard.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelFeedAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Post;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.ui.widget.timeline.TimelineAdapter;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.EndlessRecyclerViewScrollListener;
import im.ene.toro.Toro;
import im.ene.toro.ToroPlayer;
import im.ene.toro.ToroStrategy;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

public class ChannelFeedFragment extends Fragment {

    private List<Post> postList;
    private RecyclerView recyclerView;
    private EndlessRecyclerViewScrollListener scrollListener;
    TimelineAdapter adapter;
    private ProgressBar progressBar;
    private String channelToken;

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

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
        this.postList = new ArrayList<Post>();

//        initFeed();
        initEmptyState(view);
        getChannelFeedsNextPage(1);

        return view;
    }

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void initFeed() {
        adapter = new TimelineAdapter(getActivity(), this.postList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);

        final ToroStrategy oldStrategy = Toro.getStrategy();
        final int firstVideoPosition = adapter.firstVideoPosition();

        Toro.setStrategy(new ToroStrategy() {
            boolean isFirstPlayerDone = firstVideoPosition != -1; // Valid first position only

            @Override public String getDescription() {
                return "First video plays first";
            }

            @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
                return oldStrategy.findBestPlayer(candidates);
            }

            @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
                boolean allowToPlay = (isFirstPlayerDone || player.getPlayOrder() == firstVideoPosition)  //
                        && oldStrategy.allowsToPlay(player, parent);

                // A work-around to keep track of first video on top.
                if (player.getPlayOrder() == firstVideoPosition) {
                    isFirstPlayerDone = true;
                }
                return allowToPlay;
            }
        });

        Toro.register(recyclerView);





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
        Toro.unregister(recyclerView);
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
        initFeed();
//        recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, itemAdded);

    }
}
