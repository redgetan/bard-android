package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.FriendListAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by reg on 2017-05-12.
 */
public class ChannelMemberInviteActivity extends BaseActivity {
    private String channelToken;
    private TextView channelInviteLink;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;
    private RecyclerView recyclerView;
    private List<Friend> friendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_member_invite);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Add Members");

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");

        channelInviteLink = (TextView) findViewById(R.id.channel_invite_link);
        channelInviteLink.setText(Configuration.bardAPIBaseURL() + "/channels/" + channelToken);

        recyclerView = (RecyclerView) findViewById(R.id.friend_list);

        initEmptyState();
        initFriendList();
    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("");
        emptyStateDescription.setText("No friends yet");

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void initFriendList() {
        friendList = Friend.friendsForUser(Setting.getUsername(this));

        if (friendList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        final Context self = this;

        // set adapter
        FriendListAdapter friendListAdapter = new FriendListAdapter(this, friendList);
        friendListAdapter.setOnItemClickListener(new FriendListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Friend user) {
                // add to list of members

            }

        });
        recyclerView.setAdapter(friendListAdapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // set decorator
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    public void onBackPressed() {
        // go to home
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel_member_invite, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.menu_item_channel_invite_finish:
                // must set usernames on intent to let these people join
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
