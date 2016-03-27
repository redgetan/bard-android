package com.sandbox.myfirstapp.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.DividerItemDecoration;
import com.sandbox.myfirstapp.app.models.Repo;
import com.sandbox.myfirstapp.app.ui.adapter.RepoListAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UserRepoListActivity extends BaseActivity {

    private Context context;
    public static final String VIDEO_LOCATION_MESSAGE = "com.sandbox.myfirstapp.VIDEO_URL";

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
