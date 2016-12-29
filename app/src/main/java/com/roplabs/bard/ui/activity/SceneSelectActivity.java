package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.EndlessRecyclerViewScrollListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SceneSelectActivity extends BaseActivity {
    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public List<Scene> sceneList;
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        mContext = this;
        recyclerView = (RecyclerView) findViewById(R.id.scene_list);
        progressBar = (ProgressBar) findViewById(R.id.scene_progress_bar);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_scene);

        initScenes();

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        syncRemoteData(map);
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                returnToBardEditor();
//                return(true);
//        }
//
//        return(super.onOptionsItemSelected(item));
//    }

//    @Override
//    public void onBackPressed() {
//        returnToBardEditor();
//    }

    private void syncRemoteData(Map<String, String> options) {
        progressBar.setVisibility(View.VISIBLE);

        Call<List<Scene>> call = BardClient.getAuthenticatedBardService().listScenes(options);
        call.enqueue(new Callback<List<Scene>>() {
            @Override
            public void onResponse(Call<List<Scene>> call, Response<List<Scene>> response) {
                List<Scene> remoteSceneList = response.body();
                for (Scene scene : remoteSceneList) {
                    if (Scene.forToken(scene.getToken()) == null) {
                        // create if scene doesnt exist yet
                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        Scene.create(realm, scene.getToken(),"",scene.getName(),scene.getThumbnailUrl());
                        realm.commitTransaction();
                    }
                }
                progressBar.setVisibility(View.GONE);
                int oldPosition = sceneList.size();
                sceneList.addAll(remoteSceneList);
                recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, remoteSceneList.size());
            }

            @Override
            public void onFailure(Call<List<Scene>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (Scene.findAll().size() == 0) {
                    Toast.makeText(getApplicationContext(), "Failed to load. Make sure internet is enabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        BardLogger.log("Scene Select onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void initScenes() {
        final Context self = this;

        this.sceneList = new ArrayList<Scene>();

        // set adapter
        SceneListAdapter adapter = new SceneListAdapter(this, this.sceneList);
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent();
                if (scene != null) {
                    intent.putExtra("sceneToken", scene.getToken());
                    BardLogger.trace("[sceneSelect] " + scene.getToken());
                } else {
                    intent.putExtra("sceneToken", "");
                    BardLogger.trace("[sceneSelect] - all");
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        recyclerView.setAdapter(adapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // set decorator
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                Map<String, String> data = new HashMap<String, String>();
                data.put("page", String.valueOf(page));
                syncRemoteData(data);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
    }

}
