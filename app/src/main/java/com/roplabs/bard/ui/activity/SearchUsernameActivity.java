package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.UserListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.User;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class SearchUsernameActivity extends BaseActivity {
    private Context mContext;
    private EditText searchBar;
    private ImageView cancelSearchBtn;
    private ImageView clearSearchBtn;
    private List<User> userList;
    private RecyclerView recyclerView;
    private List<Friend> friendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_username);

        mContext = this;

        searchBar = (EditText) toolbar.findViewById(R.id.video_search_input);
        searchBar.requestFocus();
        clearSearchBtn = (ImageView) toolbar.findViewById(R.id.clear_search_btn);
        cancelSearchBtn = (ImageView) toolbar.findViewById(R.id.cancel_search_btn);
        recyclerView = (RecyclerView) findViewById(R.id.username_invite_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        friendList = Friend.friendsForUser(Setting.getUsername(this));

        initSearch();

        // show keyboard
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
    }

    private void performSearch(String search) {
        userList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();

        Call<List<User>> call = BardClient.getAuthenticatedBardService().searchUsername(search);
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                List<User> users = response.body();
                if (users != null) {
                    for (User user : users) {
                        userList.add(user);
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                }

            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
            }
        });
    }

    private void addFriend(User user) {
//        final Context self = this;
        final String username = user.getUsername();
        Call<User> call = BardClient.getAuthenticatedBardService().addFriend(username);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User user = response.body();
                if (user != null) {
                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    Friend.create(realm, username, Setting.getUsername(ClientApp.getContext()));
                    realm.commitTransaction();

                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(ClientApp.getContext(), "Unable to add " + username + " as friend", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ClientApp.getContext(), "Unable to add " + username + " as friend", Toast.LENGTH_LONG).show();
            }
        });

    }


    private void initSearch() {

        userList = new ArrayList<User>();

        // set adapter
        UserListAdapter userListAdapter = new UserListAdapter(this, userList, friendList);
        userListAdapter.setOnItemClickListener(new UserListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, User user) {
                if (isFriendsAlready(user)) {
                } else {
                    addFriend(user);
                }
            }

        });
        recyclerView.setAdapter(userListAdapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // set decorator
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    BardLogger.log("user clicked search icon ...");
                    performSearch(searchBar.getText().toString());

                    return true;
                }
                return false;
            }
        });

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    performSearch(searchBar.getText().toString());

                    return true;
                }
                return false;
            }
        });

        cancelSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        clearSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

    }

    private boolean isFriendsAlready(User user) {
        String currentUsername = Setting.getUsername(ClientApp.getContext());
        if (user.getUsername().equals(currentUsername)) {
            return true;
        }

        for (Friend friend : friendList) {
            if (friend.getFriendname().equals(user.getUsername())) {
                return true;
            }
        }

        return false;
    }
}
