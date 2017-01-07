package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.*;
import com.lapism.searchview.SearchView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.EndlessRecyclerViewScrollListener;
import com.roplabs.bard.util.Helper;
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

import static com.roplabs.bard.util.Helper.LOGIN_REQUEST_CODE;
import static com.roplabs.bard.util.Helper.REQUEST_WRITE_STORAGE;

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
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;


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
        title.setText("Bard");

        Helper.initNavigationViewDrawer(this, toolbar);
        initEmptyState();
        initSearch();
        initScenes();
        Helper.askStoragePermission(this);

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        syncRemoteData(map);
    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();


                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private void displayEmptySearchMessage() {
        emptyStateTitle.setText("No results found");
        emptyStateDescription.setText("Try another search or upload an existing video");
        emptyStateContainer.setVisibility(View.VISIBLE);
    }

    private void hideEmptySearchMessage() {
        emptyStateContainer.setVisibility(View.GONE);
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

        lastSearch = text;
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

                if (remoteSceneList == null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Request Failed");
                    emptyStateDescription.setText("Currently unable to fetch data from server. Try again later.");
                } else if (remoteSceneList.isEmpty() && options.containsKey("page") && options.get("page").equals("1")) {
                    // when performing a search, it will do two requests if initial list is < threshold for pagination trigger
                    // in that case, the 2nd request will be empty (but since first request contains results, we dont want
                    // to display no results found message
                    displayEmptySearchMessage();
                } else {
                    hideEmptySearchMessage();
                    populateScenes(remoteSceneList, options);
                }
            }

            @Override
            public void onFailure(Call<List<Scene>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyStateContainer.setVisibility(View.VISIBLE);

                if (Helper.isConnectedToInternet()) {
                    emptyStateTitle.setText("Request Failed");
                    emptyStateDescription.setText("Currently unable to fetch data from server. Try again later.");
                }

//                if (Scene.findAll().size() == 0) {
//                    Toast.makeText(getApplicationContext(), "Failed to load. Make sure internet is enabled", Toast.LENGTH_SHORT).show();
//                }
            }
        });
    }

    @Override
    protected void onResume() {
        BardLogger.log("Scene Select onResume");
        Helper.initNavigationViewDrawer(this, toolbar);
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
        } else if (resultCode == RESULT_OK && requestCode == LOGIN_REQUEST_CODE) {
        }
    }

    private void populateScenes(List<Scene> remoteSceneList, Map<String, String> options) {
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
