package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.firebase.database.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelListAdapter;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by reg on 2017-05-13.
 */
public class SendToChannelActivity  extends BaseActivity {

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;
    private List<Channel> channelList;
    private List<Channel> selectedChannels;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to_channel);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Send To");
        initEmptyState();
        initChannelList();
        fetchChannelList();

    }

    private void initChannelList() {
        final Context self = this;
        channelList = new ArrayList<Channel>();
        selectedChannels = new ArrayList<Channel>();

        recyclerView = (RecyclerView) findViewById(R.id.channel_list);
        ChannelListAdapter adapter = new ChannelListAdapter(self, channelList);
        adapter.setOnItemCheckListener(new ChannelListAdapter.OnItemCheckListener() {
            @Override
            public void onItemCheck(View itemView, int position, Channel channel, boolean isChecked) {
                if (isChecked) {
                    selectedChannels.add(channel);
                } else {
                    selectedChannels.remove(channel);
                }
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(self));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(self, R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

//        emptyStateTitle.setText("");
//        emptyStateDescription.setText("");

        emptyStateContainer.setVisibility(View.VISIBLE);
    }

    private Channel getChannelFromToken(String channelToken) {
        Channel targetChannel = null;

        for (Channel channel : channelList) {
            if (channel.getToken().equals(channelToken)) {
                targetChannel = channel;
                break;
            }
        }

        return targetChannel;
    }

    public void updateChannels(String channelToken, HashMap<String, Object> channelResult) {
        Channel channel = getChannelFromToken(channelToken);

        if (channel == null) {
            // create channel
            channel = Channel.createFromFirebase(channelToken, channelResult);
            channelList.add(channel);
        } else {
            channel.updateFromFirebase(channelResult);
        }
    }

    private void fetchChannelList() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userChannelsRef = database.getReference("users/" + Setting.getUsername(ClientApp.getContext()) + "/channels");
        userChannelsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot channelSnapshot : dataSnapshot.getChildren()) {
                    final String channelToken = channelSnapshot.getKey();
                    DatabaseReference channelRef = FirebaseDatabase.getInstance().getReference("channels/" + channelToken + "");
                    channelRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            HashMap<String, Object> channelResult = (HashMap<String, Object>) dataSnapshot.getValue();
                            updateChannels(channelToken, channelResult);
                            Collections.sort(channelList);
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void onSendToChannelClick(View view) {
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // click on 'up' button in the action bar, handle it here
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
