package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.FriendListAdapter;
import com.roplabs.bard.adapters.UserListAdapter;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.User;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.roplabs.bard.util.Helper.SEARCH_USERNAME_REQUEST_CODE;

/**
 * Created by reg on 2017-05-08.
 */
public class MessageNewActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private List<Friend> friendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("MessageNew onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_new);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("New Message");

        recyclerView = (RecyclerView) findViewById(R.id.friend_list);

//        channelNameInput = (EditText) findViewById(R.id.input_channel_name);
//        channelNameDescription = (EditText) findViewById(R.id.input_channel_description);
//        createChannelButton = (Button) findViewById(R.id.btn_create_channel);

        initMessageNew();

    }

    private void initMessageNew() {
        friendList = Friend.friendsForUser(Setting.getUsername(this));

        // set adapter
        FriendListAdapter friendListAdapter = new FriendListAdapter(this, friendList);
        friendListAdapter.setOnItemClickListener(new FriendListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Friend user) {
                // create 1-1 channel with friend on server
                // exit activity and open ChannelActivity with newtoken
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

    public void onInviteContact(View view) {
    }

    public void onSearchUsername(View view) {
        Intent intent = new Intent(this, SearchUsernameActivity.class);
        startActivityForResult(intent, SEARCH_USERNAME_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        friendList = Friend.friendsForUser(Setting.getUsername(this));
        recyclerView.getAdapter().notifyDataSetChanged();

        super.onResume();
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
