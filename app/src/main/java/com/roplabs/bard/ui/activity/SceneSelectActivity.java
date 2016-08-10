package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
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
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_scene);

        Intent intent = getIntent();
        characterToken = intent.getStringExtra("characterToken");

        final RealmResults<Scene> sceneResults = Scene.forCharacterToken(characterToken);

        RealmChangeListener changeListener = new RealmChangeListener() {
            @Override
            public void onChange() {
                displaySceneList(sceneResults);
            }
        };

        sceneResults.addChangeListener(changeListener);

        syncRemoteData();
    }

    private void syncRemoteData() {
        Call<List<Scene>> call = BardClient.getBardService().listScenes(characterToken);
        call.enqueue(new Callback<List<Scene>>() {
            @Override
            public void onResponse(Call<List<Scene>> call, Response<List<Scene>> response) {
                List<Scene> sceneList = response.body();
                Scene.copyToRealmOrUpdate(sceneList);
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


    public void displaySceneList(List<Scene> sceneList) {
        recyclerView = (RecyclerView) findViewById(R.id.scene_list);
        SceneListAdapter adapter = new SceneListAdapter(this, sceneList);
        final Context self = this;
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("characterToken", characterToken);
                intent.putExtra("sceneToken", scene.getToken());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.scene_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }


}
