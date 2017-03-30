package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class ChannelListActivity extends BaseActivity {
    private final int BARD_EDITOR_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Channel List onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_character);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) { actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp); }

        progressBar = (ProgressBar) findViewById(R.id.character_progress_bar);

        initEmptyState();

        displayChannelList();



    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("Create or Join Channels");
        emptyStateDescription.setText("Channels let you talk to other users. You can add videos to a channel, and let people talk using only words from those videos. ");

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void syncRemoteData() {

    }

    @Override
    protected void onResume() {
        // fetch from local db
        RealmResults<Channel> channels = Channel.forUsername(Setting.getUsername(this));
        ((ChannelListAdapter) recyclerView.getAdapter()).swap(channels);
        recyclerView.getAdapter().notifyDataSetChanged();

        if (channels.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }


    public void displayChannelList() {
        final List<Channel> channels = Channel.forUsername(Setting.getUsername(this));

        if (channels.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }
        BardLogger.log("displaying channels count: " + channels.size());

        recyclerView = (RecyclerView) findViewById(R.id.index_list);
        ChannelListAdapter adapter = new ChannelListAdapter(this, channels);
        final Context self = this;
        adapter.setOnItemClickListener(new ChannelListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Channel channel) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("channelToken", channel.getToken());
                BardLogger.trace("[view channel] " + channel.getToken());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == BARD_EDITOR_REQUEST_CODE) {
            finish();
        }
    }

    // http://developer.android.com/guide/topics/ui/menus.html
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            if (mDrawerLayout.isDrawerOpen(mDrawerLayout.getChildAt(1)))
//                mDrawerLayout.closeDrawers();
//            else {
//                mDrawerLayout.openDrawer(mDrawerLayout.getChildAt(1));
//            }
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


}
