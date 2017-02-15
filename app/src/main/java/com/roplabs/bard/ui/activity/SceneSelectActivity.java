package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

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
    private TextView videoListLabel;
    private TextView pickVideoLabel;

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private final static int BARD_EDITOR_REQUEST_CODE = 1;
    private String lastSearch;
    private Drawable searchIcon;
    private Drawable clearIcon;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    private HashMap<String, List<Scene>> sceneListCache;
    private int sceneListCacheExpiry;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        mContext = this;
        recyclerView = (RecyclerView) findViewById(R.id.scene_list);
        progressBar = (ProgressBar) findViewById(R.id.scene_progress_bar);
        searchBar = (EditText) findViewById(R.id.video_search_input);
        videoListLabel = (TextView) findViewById(R.id.video_list_label);
        pickVideoLabel = (TextView) findViewById(R.id.pick_video_label);
        searchIcon = searchBar.getCompoundDrawables()[LEFT_DRAWABLE_INDEX];
        clearIcon = searchBar.getCompoundDrawables()[RIGHT_DRAWABLE_INDEX];

        sceneListCache = new HashMap<String, List<Scene>>();
        sceneListCacheExpiry = Calendar.getInstance().get(Calendar.SECOND) + (60 * 60); // 1 hour

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.app_name);
        title.setTextSize(24);

        deepLinkNavigate();

        Helper.initNavigationViewDrawer(this, toolbar);
        initEmptyState();
        initSearch();
        initScenes();
        Helper.askStoragePermission(this);
    }

    private void deepLinkNavigate() {
        Intent intent = getIntent();
        String sceneToken;

        if ((sceneToken = intent.getStringExtra("sceneTokenEditorDeepLink")) != null) {
            Intent newIntent = new Intent(this, BardEditorActivity.class);
            newIntent.putExtra("sceneToken", sceneToken);
            BardLogger.trace("[sceneSelect] " + sceneToken);
            startActivityForResult(newIntent, BARD_EDITOR_REQUEST_CODE);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_search:
                if (searchBar.getVisibility() == View.GONE) {
                    // show searchbar
                    searchBar.setVisibility(View.VISIBLE);
                    videoListLabel.setVisibility(View.GONE);
                    pickVideoLabel.setVisibility(View.GONE);
                    item.setIcon(R.drawable.ic_eject_white_18dp);

                    searchBar.requestFocus();

                    // show keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
                } else {
                    // hide and reset searchbar
                    searchBar.setText("");
                    searchBar.setVisibility(View.GONE);
                    videoListLabel.setVisibility(View.GONE);
                    pickVideoLabel.setVisibility(View.VISIBLE);
                    item.setIcon(R.drawable.ic_search_white_18dp);

                    scrollListener.resetState();
                    performSearch("");

                    // hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void displayInitialSearchMessage() {
        emptyStateTitle.setText("");
        emptyStateDescription.setText("Search title or words in the video");
        emptyStateContainer.setVisibility(View.VISIBLE);
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
        hideEmptySearchMessage();

        sceneList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        JSONObject properties = new JSONObject();

        try {
            properties.put("text", text);
        } catch (JSONException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
        Analytics.track(this, "search", properties);

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        map.put("search", text);
        syncRemoteData(map);

        lastSearch = text;
    }


    private void syncRemoteData(final Map<String, String> options) {
        // check to see if already in cache (only if there's no search term)
        final List<Scene> cachedScenes = sceneListCache.get(getCacheKey(options));
        if (cachedScenes != null) {
            populateScenes(cachedScenes, options);
            return;
        }


        // fetch remote
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
                    sceneListCache.put(getCacheKey(options), remoteSceneList);
                }
            }

            @Override
            public void onFailure(Call<List<Scene>> call, Throwable t) {
                BardLogger.log("SceneRequest failed: " + t.getMessage());
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

    private String getCacheKey(Map<String, String> options) {
        String search = options.get("search");

        if (search == null) {
            return options.get("page");
        } else {
            return options.get("page") + search;
        }

    }

    @Override
    protected void onResume() {
        BardLogger.log("Scene Select onResume");

        int timeNow = Calendar.getInstance().get(Calendar.SECOND);
        if (timeNow > sceneListCacheExpiry) {
            sceneListCache.clear();
        }

        // fetch data if blank (i.e. previously no internet connection)
        if (recyclerView.getAdapter().getItemCount() == 0) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("page",String.valueOf(1));
            syncRemoteData(map);
        }

        Helper.initNavigationViewDrawer(this, toolbar);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // go to home
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
            if (scene.getToken() == null) continue;

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

}
