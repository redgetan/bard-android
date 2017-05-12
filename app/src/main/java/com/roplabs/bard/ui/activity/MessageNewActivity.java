package com.roplabs.bard.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.roplabs.bard.util.Helper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.roplabs.bard.util.Helper.*;

/**
 * Created by reg on 2017-05-08.
 */
public class MessageNewActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private List<Friend> friendList;

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("MessageNew onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_new);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("New Chat");

        recyclerView = (RecyclerView) findViewById(R.id.friend_list);

//        channelNameInput = (EditText) findViewById(R.id.input_channel_name);
//        channelNameDescription = (EditText) findViewById(R.id.input_channel_description);
//        createChannelButton = (Button) findViewById(R.id.btn_create_channel);

        initEmptyState();
        initMessageNew();

    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("");
        emptyStateDescription.setText("No friends yet.");

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void createChannel(final String receiver, String sender) {
        HashMap<String, String> options = new HashMap<String, String>();
        String participants = TextUtils.join(",", new String[] { sender, receiver });
        options.put("name", participants);
        options.put("participants", participants);
        options.put("mode", "pair");

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
            setResult(RESULT_OK);
            finish();
        } else if (resultCode == RESULT_OK && requestCode == CHANNEL_CREATE_REQUEST_CODE) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void initMessageNew() {
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
                // create 1-1 channel with friend on server
                // exit activity and open ChannelActivity with newtoken
                String receiver = user.getFriendname();
                String sender = Setting.getUsername(ClientApp.getContext());
                String[] participants = new String[] { sender, receiver };
                Arrays.sort(participants);

                Channel localChannel = Channel.forParticipants(TextUtils.join(",", participants));
                if (localChannel != null) {
                    Intent intent = new Intent(self, ChannelActivity.class);
                    intent.putExtra("channelToken", localChannel.getToken());
                    startActivityForResult(intent, CHANNEL_REQUEST_CODE);
                } else {
                    createChannel(receiver, sender);
                }
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
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, InviteContactsActivity.class);
            startActivityForResult(intent, INVITE_CONTACT_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "The app was not allowed to read your contacts. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
        } else {
            Helper.askContactsPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent intent = new Intent(this, InviteContactsActivity.class);
                    startActivityForResult(intent, INVITE_CONTACT_REQUEST_CODE);

                } else {

                    Toast.makeText(this, "The app was not allowed to read your contacts. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onSearchUsername(View view) {
        Intent intent = new Intent(this, SearchUsernameActivity.class);
        startActivityForResult(intent, SEARCH_USERNAME_REQUEST_CODE);
    }

    public void onCreateGroup(View view) {
        Intent intent = new Intent(this, ChannelCreateActivity.class);
        startActivityForResult(intent, CHANNEL_CREATE_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        friendList = Friend.friendsForUser(Setting.getUsername(this));
        recyclerView.getAdapter().notifyDataSetChanged();

        if (friendList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

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
