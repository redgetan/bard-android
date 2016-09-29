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
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class SceneSelectActivity extends BaseActivity {
    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private String characterToken;
    private String previousSceneToken;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_scene);

        progressBar = (ProgressBar) findViewById(R.id.scene_progress_bar);

        Intent intent = getIntent();
        characterToken = intent.getStringExtra("characterToken");
        previousSceneToken = intent.getStringExtra("previousSceneToken");

        RealmResults<Scene> scenes = Scene.forCharacterToken(characterToken);
        displaySceneList(scenes);

        if (scenes.size() == 0) {
            progressBar.setVisibility(View.VISIBLE);
        }

        syncRemoteData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnToBardEditor();
                return(true);
        }

        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onBackPressed() {
        returnToBardEditor();
    }

    private void returnToBardEditor() {
        Intent intent = new Intent();
        intent.putExtra("characterToken", characterToken);
        BardLogger.log("prev sceneToken: " + previousSceneToken);
        intent.putExtra("sceneToken", previousSceneToken);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void syncRemoteData() {
        Call<List<Scene>> call = BardClient.getAuthenticatedBardService().listScenes(characterToken);
        call.enqueue(new Callback<List<Scene>>() {
            @Override
            public void onResponse(Call<List<Scene>> call, Response<List<Scene>> response) {
                List<Scene> sceneList = response.body();
                Scene.setNameAndThumbnails(sceneList);
                progressBar.setVisibility(View.GONE);
                ((SceneListAdapter) recyclerView.getAdapter()).swap(sceneList);
            }

            @Override
            public void onFailure(Call<List<Scene>> call, Throwable t) {
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

    public void displaySceneList(List<Scene> sceneList) {
        BardLogger.log("displaying scenes count: " + sceneList.size());

        recyclerView = (RecyclerView) findViewById(R.id.scene_list);
        SceneListAdapter adapter = new SceneListAdapter(this, sceneList);
        final Context self = this;
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent();
                intent.putExtra("characterToken", characterToken);
                if (scene != null) {
                    intent.putExtra("sceneToken", scene.getToken());
                } else {
                    intent.putExtra("sceneToken", "");
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }


}
