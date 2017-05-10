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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
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
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Like;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.DividerItemDecoration;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.adapters.RepoListAdapter;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.EndlessRecyclerViewScrollListener;
import com.roplabs.bard.util.Helper;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.roplabs.bard.util.Helper.VIDEO_PLAYER_REQUEST_CODE;

public class RepoListActivity extends BaseActivity {


    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    private Button emptyStartBtn;
    private RepoListAdapter adapter;
    public static final String VIDEO_LOCATION_MESSAGE = "com.roplabs.bard.VIDEO_URL";
    public static final String REPO_TOKEN_MESSAGE = "com.roplabs.bard.REPO_TOKEN";
    public static final String REPO_URL_MESSAGE = "com.roplabs.bard.REPO_URL";
    private String repoListType;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private List<Repo> repoList;
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        Intent intent = getIntent();
        repoListType = intent.getStringExtra("repoListType");

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);

        recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // http://stackoverflow.com/a/27037230
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        progressBar = (ProgressBar) findViewById(R.id.repo_progress_bar);


        if (repoListType.equals("likes")) {
            title.setText(R.string.my_likes);
        } else {
            title.setText(R.string.bard_library);
        }

        initEmptyState();
        initRepos();
        displayRepoList();

    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_repo_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_repo_title);
        emptyStateContainer.setVisibility(View.GONE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == VIDEO_PLAYER_REQUEST_CODE) {
            if (data != null) {
                boolean isUnliked = data.getBooleanExtra("unliked", false);
                if (isUnliked) {
                    refreshList();
                    return;
                }

                boolean isDeleted = data.getBooleanExtra("deleted", false);
                if (isDeleted) {
                    refreshList();
                }
            }
        }
    }

    private void refreshList() {
        repoList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        displayRepoList();
    }

    private void toggleEmptyState(List<Repo> repos) {
        if (repos.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    public void displayUserCreatedRepo() {
        final List<Repo> repos;

        repos = Repo.forUsername(Setting.getUsername(this));

        for (Repo repo : repos) {
            repoList.add(repo);
        }
        toggleEmptyState(repoList);

        recyclerView.getAdapter().notifyItemRangeInserted(0, repoList.size());
    }

    public void initRepos() {
        this.repoList = new ArrayList<Repo>();
        final Context self = this;

        adapter = new RepoListAdapter(this, this.repoList);
        adapter.setOnItemClickListener(new RepoListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Repo repo) {
                Intent intent = new Intent(self, VideoPlayerActivity.class);
                intent.putExtra("title", repo.title());
                intent.putExtra("repoUsername", repo.getUsername());
                intent.putExtra("repoListType", repoListType);
                if (repo.getFilePath().isEmpty()) {
                    intent.putExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE, repo.getSourceUrl());
                } else {
                    intent.putExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE, repo.getFilePath());
                }
                intent.putExtra(RepoListActivity.REPO_URL_MESSAGE, repo.getUrl());
                intent.putExtra(RepoListActivity.REPO_TOKEN_MESSAGE, repo.getToken());
                startActivityForResult(intent, VIDEO_PLAYER_REQUEST_CODE);
            }
        });

        recyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(self);
        recyclerView.setLayoutManager(layoutManager);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                BardLogger.log("LOAD_MORE: " + page);
                getReposNextPage(page);
            }
        };

        if (repoListType.equals("likes")) {
            recyclerView.addOnScrollListener(scrollListener);
        }
    }

    private void getReposNextPage(int page) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("page", String.valueOf(page));

        syncRemoteData(data);
    }

    public void displayUserLikes() {
        final List<Repo> repos;
        Map<String, String> options = new HashMap<String, String>();

        List<String> repoTokens = Like.repoTokensForUsername(Setting.getUsername(this));

        if (!Setting.isLogined(this)) {
            options.put("repoTokens", TextUtils.join(",", repoTokens));
        }

        syncRemoteData(options);
    }

    public void syncRemoteData(final Map<String, String> options) {
        // fetch remote
        progressBar.setVisibility(View.VISIBLE);

        BardLogger.log("SYNC_REMOTE_DATA..");

        Call<List<Repo>> call = BardClient.getAuthenticatedBardService().listLikes(options);
        call.enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                progressBar.setVisibility(View.GONE);

                List<Repo> remoteRepoList = response.body();

                if (remoteRepoList == null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Request Failed");
                } else {
                    if (remoteRepoList.isEmpty() && repoList.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                    } else {
                        emptyStateContainer.setVisibility(View.GONE);
                        populateRepos(remoteRepoList);
                    }
                }

            }

            @Override
            public void onFailure(Call<List<Repo>> call, Throwable t) {
                BardLogger.log("SceneRequest failed: " + t.getMessage());
                progressBar.setVisibility(View.GONE);
                emptyStateContainer.setVisibility(View.VISIBLE);

                if (Helper.isConnectedToInternet()) {
                    emptyStateTitle.setText("Request Failed");
                } else {
                    emptyStateTitle.setText(R.string.no_network_connection);
                }
            }
        });
    }

    private void populateRepos(List<Repo> remoteRepoList) {

        int itemAdded = 0;

        int oldPosition = repoList.size();

        // create repos if not in device
        for (Repo remoteRepo : remoteRepoList) {
            if (remoteRepo.getToken() == null) continue;

            Repo localRepo = Repo.forToken(remoteRepo.getToken());
            if (localRepo == null) {
                Repo repo = Repo.createFromOtherUser(remoteRepo.getToken(), remoteRepo.getUrl(), remoteRepo.getUUID(),
                         remoteRepo.getCharacterToken(), remoteRepo.getSceneToken(), "", remoteRepo.getWordList(), remoteRepo.getCreatedAt(),
                        remoteRepo.getUsername(), remoteRepo.getThumbnailUrl(), remoteRepo.getSourceUrl());
            }


        }

        // create likes if not on device
        Like.createOrUpdate(remoteRepoList);


        for (Repo remoteRepo : remoteRepoList) {
            if (!repoList.contains(remoteRepo)) {
                repoList.add(remoteRepo);
                itemAdded++;
            }
        }
        recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, itemAdded);
    }

    public void displayRepoList() {
        if (repoListType.equals("likes")) {
            displayUserLikes();
        } else {
            displayUserCreatedRepo();
        }

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
