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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.database.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.FriendListAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import org.w3c.dom.Text;

import java.util.ArrayList;
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
    private List<Friend> friendsToAddInGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_member_invite);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Add Members");

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        boolean hideInviteLink = intent.getBooleanExtra("hideInviteLink", false);

        if (hideInviteLink) {
            LinearLayout channelInviteLinkContainer = (LinearLayout) findViewById(R.id.channel_invite_link_container);
            channelInviteLinkContainer.setVisibility(View.GONE);
        } else {
            channelInviteLink = (TextView) findViewById(R.id.channel_invite_link);
            channelInviteLink.setText(Configuration.bardAPIBaseURL() + "/channels/" + channelToken);
        }

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

    interface OnFriendListValidated {
        void onFriendListValidated();
    }


    private void removeMembersFromFriendList(final OnFriendListValidated callback) {
        // only show friends that are not member yet
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference channelMembersRef = database.getReference("channels/" + channelToken + "/participants");
        channelMembersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String username = memberSnapshot.getKey();
                    Friend member = new Friend(username, Setting.getUsername(ClientApp.getContext()));
                    int memberIndex = friendList.indexOf(member);
                    if (memberIndex != -1) {
                        friendList.remove(memberIndex);
                    }

                }

                callback.onFriendListValidated();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onFriendListValidated();
            }
        });
    }

    private void initFriendList() {
        final Context self = this;

        friendList = Friend.friendsForUser(Setting.getUsername(this));

        removeMembersFromFriendList(new OnFriendListValidated() {
            @Override
            public void onFriendListValidated() {
                if (friendList.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                }

                // set adapter
                FriendListAdapter friendListAdapter = new FriendListAdapter(self, friendList, "add_to_group");
                friendListAdapter.setOnItemCheckClickListener(new FriendListAdapter.OnItemCheckClickListener() {
                    @Override
                    public void onItemCheckClick(View itemView, int position, Friend user, boolean isChecked) {
                        // add to list of members
                        if (isChecked) {
                            friendsToAddInGroup.add(user);
                        } else {
                            friendsToAddInGroup.remove(user);
                        }
                    }

                });
                recyclerView.setAdapter(friendListAdapter);

                // set layout manager
                LinearLayoutManager layoutManager = new LinearLayoutManager(self);
                recyclerView.setLayoutManager(layoutManager);

                // set decorator
                ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(self, R.dimen.scene_item_offset);
                recyclerView.addItemDecoration(itemDecoration);
            }
        });

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

    private void addFriendsToGroup() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference channelMembersRef;
        DatabaseReference userChannelsRef;
        for (Friend user : friendsToAddInGroup) {
            channelMembersRef = database.getReference("channels/" + channelToken + "/participants/" + user.getFriendname());
            userChannelsRef   = database.getReference("users/" + user.getFriendname() + "/channels/" + channelToken);
            channelMembersRef.setValue(true);
            userChannelsRef.setValue(true);
        }
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
                addFriendsToGroup();
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
