package com.roplabs.madchat.ui;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;
import com.roplabs.madchat.R;
import com.roplabs.madchat.api.MadchatClient;
import com.roplabs.madchat.events.IndexFetchEvent;
import com.roplabs.madchat.models.Index;
import com.roplabs.madchat.models.ItemOffsetDecoration;
import com.roplabs.madchat.ui.adapter.IndexListAdapter;
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
