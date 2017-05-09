package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.FriendListAdapter;
import com.roplabs.bard.adapters.UserListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.User;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.roplabs.bard.util.Helper.CHANNEL_REQUEST_CODE;
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

    private void createChannel(final String receiver, String sender) {
        HashMap<String, String> options = new HashMap<String, String>();
        String participants = TextUtils.join(",", new String[] { sender, receiver });
        options.put("name", participants);
        options.put("participants", participants);
        options.put("type", "pair");

        final Context self = this;

        Call<Channel> call = BardClient.getAuthenticatedBardService().createChannel(options);
        call.enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {
                Channel channel = response.body();
                if (channel != null) {
                    Channel localChannel = Channel.forToken(channel.getToken());
                    if (localChannel == null) {
                        // create local one
                        localChannel = Channel.create(channel);
                    }

                    Intent intent = new Intent(self, ChannelActivity.class);
                    intent.putExtra("channelToken", localChannel.getToken());
                    startActivityForResult(intent, CHANNEL_REQUEST_CODE);
                } else {
                    Toast.makeText(ClientApp.getContext(), "Unable to chat with " + receiver, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {
                Toast.makeText(ClientApp.getContext(), "Unable to chat with " + receiver, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHANNEL_REQUEST_CODE) {
            // if just finished chatting, go back to channel list
            finish();
        }
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
                createChannel(user.getUsername(), Setting.getUsername(ClientApp.getContext()));
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
