package com.sandbox.myfirstapp.app.ui;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.api.MadchatClient;
import com.sandbox.myfirstapp.app.events.IndexFetchEvent;
import com.sandbox.myfirstapp.app.models.Index;
import com.sandbox.myfirstapp.app.models.ItemOffsetDecoration;
import com.sandbox.myfirstapp.app.models.Setting;
import com.sandbox.myfirstapp.app.models.SquareAlbumDecoration;
import com.sandbox.myfirstapp.app.ui.adapter.IndexListAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class IndexActivity extends BaseActivity {
    private final int NUM_GRID_COLUMNS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        RealmResults<Index> indexResults = Index.findAll();
        displayIndexList(indexResults);

//        try {
//            getIndexList();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private void getIndexList() throws IOException {
        MadchatClient.getIndexList();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(IndexFetchEvent event) {
        if (event.error != null) {
            Toast.makeText(getApplicationContext(), event.error, Toast.LENGTH_SHORT).show();
            return;
        }

        displayIndexList(event.indexList);
    }

    public void displayIndexList(List<Index> indexList) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.index_list);
        IndexListAdapter adapter = new IndexListAdapter(this, indexList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, NUM_GRID_COLUMNS));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

}
