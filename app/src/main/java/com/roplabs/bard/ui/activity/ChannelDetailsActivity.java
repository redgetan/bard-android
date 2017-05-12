package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import com.google.firebase.database.*;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.UserListAdapter;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.User;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;

import java.util.ArrayList;
import java.util.List;

public class ChannelDetailsActivity extends BaseActivity {
    private String channelToken;
    private Channel channel;
    private RecyclerView recyclerView;
    private List<User> memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_details);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channel = Channel.forToken(channelToken);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Info");

        recyclerView = (RecyclerView) findViewById(R.id.member_list);

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
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference channelMembersRef = database.getReference("channels/" + channelToken + "/participants");
        channelMembersRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String username = dataSnapshot.getKey();
                User user = new User(username);
                int oldPosition = memberList.size();
                memberList.add(user);
                recyclerView.getAdapter().notifyItemRangeInserted(oldPosition,1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
