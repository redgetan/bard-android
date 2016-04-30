package com.roplabs.madchat.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.roplabs.madchat.R;
import com.roplabs.madchat.models.DividerItemDecoration;
import com.roplabs.madchat.models.Repo;
import com.roplabs.madchat.ui.adapter.RepoListAdapter;

import java.util.List;

public class RepoListActivity extends BaseActivity {

    private Context context;
    public static final String VIDEO_LOCATION_MESSAGE = "com.roplabs.madchat.VIDEO_URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        displayRepoList();
    }

    public void displayRepoList() {
        final List<Repo> repos = Repo.findAll();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        RepoListAdapter adapter = new RepoListAdapter(this, repos);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // http://stackoverflow.com/a/27037230
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
    }

}