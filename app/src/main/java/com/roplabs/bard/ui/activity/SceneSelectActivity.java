package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.lapism.searchview.SearchView;
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

public class SceneSelectActivity extends BaseActivity  {
    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public List<Scene> sceneList;
    private EndlessRecyclerViewScrollListener scrollListener;
    private EditText searchBar;

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private final static int BARD_EDITOR_REQUEST_CODE = 1;
    private String lastSearch;
    private Drawable searchIcon;
    private Drawable clearIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        SearchView view;
        mContext = this;
        recyclerView = (RecyclerView) findViewById(R.id.scene_list);
        progressBar = (ProgressBar) findViewById(R.id.scene_progress_bar);
        searchBar = (EditText) findViewById(R.id.video_search_input);
        searchIcon = searchBar.getCompoundDrawables()[LEFT_DRAWABLE_INDEX];
        clearIcon = searchBar.getCompoundDrawables()[RIGHT_DRAWABLE_INDEX];

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_scene);

        initSearch();
        initScenes();

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        syncRemoteData(map);
    }

    private void initSearch() {
        lastSearch = "";
        searchIcon.setAlpha(100); // make search icon opacity set to 50%
        clearIcon.setAlpha(0); // invisible at beginning

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    BardLogger.log("user clicked search icon ...");
                    performSearch(v.getText().toString());
                    return true;
                }
                return false;
            }
        });

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    BardLogger.log("user pressed ENTER searching...");
                    performSearch(searchBar.getText().toString());
                    return true;
                }
                if (!searchBar.getText().toString().isEmpty()) {
                    clearIcon.setAlpha(100); // make it visible if there's text
                } else {
                    clearIcon.setAlpha(0); // hide if empty
                }
                return false;
            }
        });

        searchBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (searchBar.getRight() - clearIcon.getBounds().width())) {
                        // clear button clicked
                        searchBar.setText("");
                        clearIcon.setAlpha(0); // hide clear button again
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void performSearch(String text) {
        if (lastSearch.equals(text)) return; // avoids accidental DDOS

        sceneList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        map.put("search", text);
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

    private void syncRemoteData(final Map<String, String> options) {
        progressBar.setVisibility(View.VISIBLE);

        BardLogger.log("SYNC_REMOTE_DATA..");
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

                // save last search
                if (options.get("search") != null) {
                    lastSearch = options.get("search");
                }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == BARD_EDITOR_REQUEST_CODE) {
            finish();
        }
    }

    public void initScenes() {
        final Context self = this;

        this.sceneList = new ArrayList<Scene>();

        // set adapter
        SceneListAdapter adapter = new SceneListAdapter(this, this.sceneList);
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("sceneToken", scene.getToken());
                BardLogger.trace("[sceneSelect] " + scene.getToken());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
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
                BardLogger.log("LOAD_MORE: " + page);
                getScenesNextPage(page);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
    }

    private void getScenesNextPage(int page) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("page", String.valueOf(page));

        String search = searchBar.getText().toString();

        if (!search.isEmpty()) {
            data.put("search", search);
        }

        syncRemoteData(data);
    }

//    @Override
//    public void onSearchStateChanged(boolean b) {
//
//    }
//
//    @Override
//    public void onSearchConfirmed(CharSequence charSequence) {
//
////        startSearch(charSequence.toString(), true, null, true);
//        System.out.println("SERACHING AUTOBOTS");
//    }
//
//    @Override
//    public void onButtonClicked(int i) {
//
//    }
}
