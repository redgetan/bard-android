package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
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
    private Character character;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        Intent intent = getIntent();
        character = (Character) intent.getSerializableExtra("Character");

        RealmResults<Scene> sceneResults = Scene.findAll();
        displaySceneList(sceneResults);

    }

    private void getSceneList() throws IOException {
        Call<List<Scene>> call = BardClient.getBardService().listScenes(character.getToken());
        call.enqueue(new Callback<List<Scene>>() {
            @Override
            public void onResponse(Call<List<Scene>> call, Response<List<Scene>> response) {
                List<Scene> sceneList = response.body();
                displaySceneList(sceneList);
            }

            @Override
            public void onFailure(Call<List<Scene>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "failure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }


    public void displaySceneList(List<Scene> sceneList) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.index_list);
        SceneListAdapter adapter = new SceneListAdapter(this, sceneList);
        final Context self = this;
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("Character", character);
                intent.putExtra("Scene", scene);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }


}
