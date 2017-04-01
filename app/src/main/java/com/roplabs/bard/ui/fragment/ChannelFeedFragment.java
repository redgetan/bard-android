package com.roplabs.bard.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelFeedAdapter;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.EndlessRecyclerViewScrollListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChannelFeedFragment extends Fragment {

    private List<Repo> repoList;
    private RecyclerView recyclerView;
    private EndlessRecyclerViewScrollListener scrollListener;

    public static ChannelFeedFragment newInstance() {
        Bundle args = new Bundle();
        ChannelFeedFragment fragment = new ChannelFeedFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_channel_feed, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.channel_feed_list);
        initFeed();
        getChannelFeedsNextPage(1);

        return view;
    }

    private void initFeed() {
        this.repoList = new ArrayList<Repo>();

        // set adapter
        ChannelFeedAdapter adapter = new ChannelFeedAdapter(getActivity(), this.repoList);
//        adapter.setOnItemClickListener(new ChannelFeedAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(View itemView, int position, Repo repo) {
//                parentListener.onItemClick(scene);
//            }
//        });
        recyclerView.setAdapter(adapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

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

    private void getChannelFeedsNextPage(int page) {
        if (page == 1) {
            List<Repo> repos = Repo.forUsername(Setting.getUsername(getActivity()));
            populateFeed(repos);
        }
    }

    private void populateFeed(List<Repo> remoteRepoList) {
        int oldPosition = repoList.size();
        int itemAdded = 0;
        for (Repo remoteRepo : remoteRepoList) {
            if (!repoList.contains(remoteRepo)) {
                repoList.add(remoteRepo);
                itemAdded++;
            }
        }
        recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, itemAdded);

    }
}
