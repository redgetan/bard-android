package com.sandbox.myfirstapp.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.Repo;
import com.sandbox.myfirstapp.app.ui.adapter.RepoListAdapter;
import com.sandbox.myfirstapp.app.util.ItemClickSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UserRepoListActivity extends AppCompatActivity {

    public static final String VIDEO_LOCATION_MESSAGE = "com.sandbox.myfirstapp.VIDEO_URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        final Context mContext = this;
        final List<Repo> repos = getUserRepoList();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        RepoListAdapter adapter = new RepoListAdapter(repos);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(
            new ItemClickSupport.OnItemClickListener() {
                @Override
                public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                    // do it
                    Repo repo = repos.get(position);
                    Intent intent = new Intent(mContext, VideoPlayerActivity.class);
                    intent.putExtra(VIDEO_LOCATION_MESSAGE, repo.getFilePath());
                    startActivity(intent);
                }
            }
        );
    }

    public List<Repo> getUserRepoList() {
        ArrayList<Repo> repos = new ArrayList<Repo>();
        JSONObject json = loadUserRepoJSON();
        try {
            JSONArray jArr = json.getJSONArray("projects");
            for (int i=0; i < jArr.length(); i++) {
                JSONObject obj = jArr.getJSONObject(i);
                Repo repo = new Repo();
                repo.setFilePath(obj.getString("file_path"));
                repo.setWordList(obj.getString("word_list"));
                repos.add(repo);
            }

        } catch (JSONException e) {

        }

        return repos;
    }

    public JSONObject loadUserRepoJSON() {
        try {


            String packageDir = getExternalFilesDir(null).getAbsolutePath();
            File file = new File(packageDir, "projects.json");
            InputStream is = new FileInputStream(file);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String content = new String(buffer, "UTF-8");

            return new JSONObject(content);
        } catch (IOException e) {
            return new JSONObject();
        } catch (JSONException e) {
            return new JSONObject();
        }

    }


    public void viewRepo(View view) {
    }
}
