package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.ChannelActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static com.roplabs.bard.util.Helper.CHANNEL_REQUEST_CODE;

/**
 * Created by reg on 2017-05-06.
 */
public class ChannelListFragment extends Fragment {

    private String defaultChannelToken;
    private RecyclerView recyclerView;
    private List<Channel> channelList;

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    public static ChannelListFragment newInstance() {
        Bundle args = new Bundle();
        ChannelListFragment fragment = new ChannelListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        defaultChannelToken = Configuration.mainChannelToken();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_channel_list, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.channel_list);
        channelList = new ArrayList<Channel>();

        initEmptyState(view);

        displayChannelList(view);
        if (Setting.isLogined(ClientApp.getContext())) {
            syncRemoteData();
        }

        return view;
    }

    public void displayChannelList(View view) {
        final Context self = getActivity();

        final List<Channel> channels = Channel.forUsername(Setting.getUsername(ClientApp.getContext()));

        BardLogger.log("displaying channels count: " + channels.size());

        recyclerView = (RecyclerView) view.findViewById(R.id.channel_list);
        ChannelListAdapter adapter = new ChannelListAdapter(self, channels);
        adapter.setOnItemClickListener(new ChannelListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Channel channel) {
                Intent intent = new Intent(self, ChannelActivity.class);
                intent.putExtra("channelToken", channel.getToken());
                BardLogger.trace("[view channel] " + channel.getToken());
                startActivityForResult(intent, CHANNEL_REQUEST_CODE);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(self));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(self, R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    public void onResume() {
        // fetch from local db
        RealmResults<Channel> channels = Channel.forUsername(Setting.getUsername(ClientApp.getContext()));
        ((ChannelListAdapter) recyclerView.getAdapter()).swap(channels);
        recyclerView.getAdapter().notifyDataSetChanged();

        if (channels.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        super.onResume();
    }

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("Party Time");
        emptyStateDescription.setText("Create funny videos with friends or other users ");

        emptyStateContainer.setVisibility(View.GONE);
        emptyStateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyStateContainer.setVisibility(View.GONE);
                getChannelListNextPage(1);
            }
        });
    }

    private void syncRemoteData() {

        final String username = Setting.getUsername(ClientApp.getContext());
        Call<List<Channel>> call = BardClient.getAuthenticatedBardService().listChannels(username);
        call.enqueue(new Callback<List<Channel>>() {
            @Override
            public void onResponse(Call<List<Channel>> call, Response<List<Channel>> response) {
                List<Channel> channelList = response.body();

                if (channelList == null) {
                    return;
                }


                Channel.createOrUpdate(channelList);

                if (channelList.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                }

                ((ChannelListAdapter) recyclerView.getAdapter()).swap(channelList);
            }

            @Override
            public void onFailure(Call<List<Channel>> call, Throwable t) {
            }
        });
    }

    private void getChannelListNextPage(int page) {
        syncRemoteData();

    }
}
