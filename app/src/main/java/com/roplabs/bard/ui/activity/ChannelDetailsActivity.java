package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.firebase.database.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.UserListAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.User;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;

import java.util.ArrayList;
import java.util.List;

import static com.roplabs.bard.util.Helper.CHANNEL_MEMBER_INVITE_REQUEST_CODE;

public class ChannelDetailsActivity extends BaseActivity {
    private String channelToken;
    private Channel channel;
    private RecyclerView recyclerView;
    private List<User> memberList;
    private TextView memberCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_details);

        Intent intent = getIntent();
        channel = (Channel) intent.getParcelableExtra("channel");
        channelToken = channel.getToken();


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Group Info");

        recyclerView = (RecyclerView) findViewById(R.id.member_list);
        memberCount = (TextView) findViewById(R.id.member_count);
        TextView groupName = (TextView) findViewById(R.id.group_name);
        groupName.setText(channel.getName());

        TextView channelInviteLink = (TextView) findViewById(R.id.channel_invite_link);
        channelInviteLink.setText(Configuration.bardAPIBaseURL() + "/channels/" +  channelToken);

        initMembers();
        fetchMemberList();
    }

    private void initMembers() {
        memberList = new ArrayList<User>();

        final Context self = this;

        // set adapter
        UserListAdapter userListAdapter = new UserListAdapter(this, memberList, "group_members");
        recyclerView.setAdapter(userListAdapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // set decorator
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }


    private void fetchMemberList() {
        memberList.clear();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference channelMembersRef = database.getReference("channels/" + channelToken + "/participants");
        channelMembersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String username = memberSnapshot.getKey();
                    User user = new User(username);
                    memberList.add(user);
                }

                memberCount.setText(String.valueOf(memberList.size()));
                recyclerView.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                // click on 'up' button in the action bar, handle it here
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onAddMembers(View view) {
        Intent intent = new Intent(this, ChannelMemberInviteActivity.class);
        intent.putExtra("channelToken", channelToken);
        intent.putExtra("hideInviteLink", true);
        startActivityForResult(intent, CHANNEL_MEMBER_INVITE_REQUEST_CODE);
    }

    public void onLeaveGroup(View view) {
        leaveChannel(channelToken);

        Intent intent = new Intent();
        intent.putExtra("leaveChannel", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void leaveChannel(String channelToken) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userChannelRef = database.getReference("users/" + Setting.getUsername(ClientApp.getContext()) + "/channels/" + channelToken);
        userChannelRef.removeValue();

        DatabaseReference channelMemberRef = database.getReference("channels/" + channelToken + "/participants/" + Setting.getUsername(ClientApp.getContext()));
        channelMemberRef.removeValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHANNEL_MEMBER_INVITE_REQUEST_CODE) {
            fetchMemberList();
        }
    }
}
