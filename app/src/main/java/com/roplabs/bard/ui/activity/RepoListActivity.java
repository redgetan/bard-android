package com.roplabs.bard.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.DividerItemDecoration;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.adapters.RepoListAdapter;
import com.roplabs.bard.util.BardLogger;

import java.util.List;

public class RepoListActivity extends BaseActivity {


    private FrameLayout emptyStateContainer;
    private Button emptyStartBtn;
    private RepoListAdapter adapter;
    public static final String VIDEO_LOCATION_MESSAGE = "com.roplabs.bard.VIDEO_URL";
    public static final String REPO_TOKEN_MESSAGE = "com.roplabs.bard.REPO_TOKEN";
    public static final String REPO_URL_MESSAGE = "com.roplabs.bard.REPO_URL";
    private String repoListType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        Intent intent = getIntent();
        repoListType = intent.getStringExtra("repoListType");
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);

        if (repoListType.equals("likes")) {
            title.setText(R.string.my_likes);
        } else {
            title.setText(R.string.bard_library);
        }

        initEmptyState();
        displayRepoList();

    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_repo_container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean wasEmptyBeforeButFilledNow = adapter == null && !Repo.forUsername(Setting.getUsername(this)).isEmpty();
        boolean wasNotEmptyBefore = adapter != null;

        if (wasEmptyBeforeButFilledNow) {
            displayRepoList();
        } else if (wasNotEmptyBefore) {
            adapter.notifyDataSetChanged();
        }
    }


    public void displayRepoList() {
        final List<Repo> repos;
        if (repoListType.equals("likes")) {
            repos = Repo.likesForUsername(Setting.getUsername(this));
        } else {
            repos = Repo.forUsername(Setting.getUsername(this));
        }

        if (repos.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        final Context self = this;

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        adapter = new RepoListAdapter(this, repos);
        adapter.setOnItemClickListener(new RepoListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Repo repo) {
                Intent intent = new Intent(self, VideoPlayerActivity.class);
                intent.putExtra("title", repo.title());
                intent.putExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE, repo.getFilePath());
                intent.putExtra(RepoListActivity.REPO_URL_MESSAGE, repo.getUrl());
                intent.putExtra(RepoListActivity.REPO_TOKEN_MESSAGE, repo.getToken());
                self.startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));



        // http://stackoverflow.com/a/27037230
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
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
