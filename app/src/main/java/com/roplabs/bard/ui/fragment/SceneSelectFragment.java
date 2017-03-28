package com.roplabs.bard.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.adapters.SearchListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Favorite;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.BardEditorActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import io.realm.RealmResults;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

public class SceneSelectFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";
    public static final String SCENE_TYPE = "SCENE_TYPE";

    private int mPage;
    private boolean isVisibleToUser = false;

    private final static int BARD_EDITOR_REQUEST_CODE = 1;

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    private String lastSearch;
    private HashMap<String, List<Scene>> sceneListCache;
    private int sceneListCacheExpiry;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public List<Scene> sceneList;
    private EndlessRecyclerViewScrollListener scrollListener;
    private String sceneType;
    private EditText searchBar;
    private Spinner searchTypeSpinner;

    private SearchListAdapter searchListAdapter;

    public static SceneSelectFragment newInstance(String sceneType, int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        args.putString(SCENE_TYPE, sceneType);
        SceneSelectFragment fragment = new SceneSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
        sceneType = getArguments().getString(SCENE_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scene_list, container, false);
//        TextView textView = (TextView) view;
//        textView.setText("Fragment #" + mPage);

        recyclerView = (RecyclerView) view.findViewById(R.id.scene_list);
        progressBar = (ProgressBar) view.findViewById(R.id.scene_progress_bar);
        searchBar = (EditText) view.findViewById(R.id.scene_search_input);
        searchTypeSpinner = (Spinner) view.findViewById(R.id.scene_search_spinner);
        sceneListCache = new HashMap<String, List<Scene>>();
        sceneListCacheExpiry = Calendar.getInstance().get(Calendar.SECOND) + (60 * 60); // 1 hour

        initEmptyState(view);
        initScenes();
        initSearch();


        return view;
    }

    private void initSearch() {
        lastSearch = "";

        // hide keyboard on focus out
        final Context self = this.getActivity();
        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                        InputMethodManager imm = (InputMethodManager) self.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                }
            }
        });
    }

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    public void performSearch(String text) {
        if (lastSearch.equals(text)) return; // avoids accidental DDOS
        hideEmptySearchMessage();

        sceneList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        JSONObject properties = new JSONObject();
        Bundle params = new Bundle();

        try {
            properties.put("text", text);
            params.putString("text", text);
        } catch (JSONException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
        Analytics.track(ClientApp.getContext(), "search", properties);
        Analytics.track(ClientApp.getContext(), "search", params);

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        map.put("search", text);
        map.put("type",getSearchType());
        syncRemoteData(map);

        lastSearch = text;
    }

    private String getSearchType() {
        return searchTypeSpinner.getSelectedItem().toString().toLowerCase();
    }

    private void hideEmptySearchMessage() {
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void displayEmptySearchMessage() {
        emptyStateTitle.setText("No results found");
        emptyStateDescription.setText("Try another search or upload an existing video");
        emptyStateContainer.setVisibility(View.VISIBLE);
    }


    public void syncRemoteData(final Map<String, String> options) {
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
                } else if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
                    // maybe empty on server (i.e. page 2 empty), but if page 1 has results or local db has favorites, then dont say empty
                    if (remoteSceneList.isEmpty() && sceneList.isEmpty()) {
                    } else {
                        // add remote favorite to local if missing
                        hideEmptySearchMessage();
                        populateScenes(remoteSceneList, options);
                        syncRemoteFavoritesToLocal(remoteSceneList);
                        sceneListCache.put(getCacheKey(options), remoteSceneList);
                    }
                } else if (remoteSceneList.isEmpty() && options.containsKey("page") && options.get("page").equals("1")) {

                    // when performing a search, it will do two requests if initial list is < threshold for pagination trigger
                    // in that case, the 2nd request will be empty (but since first request contains results, we dont want
                    // to display no results found message
                    // ONLY TRIGGER THIS FOR REMOTE SEARCH ON PUBLIC BOARD THOUGH, if its searching on own board, empty search doesnt make sense??
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

    private void syncRemoteFavoritesToLocal(List<Scene> remoteSceneList) {
        Favorite.createOrUpdate(remoteSceneList);
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
        int itemAdded = 0;
        for (Scene remoteScene : remoteSceneList) {
            if (!sceneList.contains(remoteScene)) {
                sceneList.add(remoteScene);
                itemAdded++;
            }
        }
        recyclerView.getAdapter().notifyItemRangeInserted(oldPosition, itemAdded);

    }

    public void initScenes() {
        final Context self = getActivity();

        this.sceneList = new ArrayList<Scene>();

        // set adapter
        SceneListAdapter adapter = new SceneListAdapter(self, this.sceneList);
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("characterToken", "");
                intent.putExtra("sceneToken", scene.getToken());
                BardLogger.trace("[sceneSelect] " + scene.getToken());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
            }
        });
        recyclerView.setAdapter(adapter);

        // set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(self);
        recyclerView.setLayoutManager(layoutManager);

        // set decorator
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(self, R.dimen.scene_item_offset);
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
        data.put("category", sceneType);

        String search = getSearchQuery();

        if (!search.isEmpty()) {
            data.put("search", search);
            data.put("type", getSearchType());
        }

        syncRemoteData(data);
    }

    private String getSearchQuery() {
        return searchBar.getText().toString();
    }

    private String getCacheKey(Map<String, String> options) {
        String search = options.get("search");
        String searchType = options.get("type");

        String cacheKey = options.get("page");

        if (search != null) {
            cacheKey = cacheKey + search + searchType;
        }

        return cacheKey;

    }

    @Override
    public void onResume() {
        BardLogger.log("SceneSelect onResume");

        int timeNow = Calendar.getInstance().get(Calendar.SECOND);
        if (timeNow > sceneListCacheExpiry) {
            sceneListCache.clear();
        }


        // for category "popular" (we want to also display results right away on fragment visible)
        // fetch data if blank (i.e. previously no internet connection)
        if (sceneType.equals(Helper.POPULAR_SCENE_TYPE) && recyclerView.getAdapter().getItemCount() == 0) {
            displayResults();
        } else if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
            displayResults();
        } else {

        }


        super.onResume();
    }

    public void displayFavorites() {
        // fetch from local db
        RealmResults<Scene> localFavorites = Scene.favoritesForUsername(Setting.getUsername(getActivity()));
        this.sceneList = new ArrayList<Scene>(localFavorites);
        SceneListAdapter adapter = new SceneListAdapter(getActivity(), this.sceneList);
        recyclerView.setAdapter(adapter);
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        if (localFavorites.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            emptyStateTitle.setText("No Bookmarks");
            emptyStateDescription.setText("Start adding videos that you like to your bookmarks");
        } else {
            hideEmptySearchMessage();
        }
    }

    public void displayResults() {
        if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
            displayFavorites();
            return;
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        map.put("category", sceneType);
        syncRemoteData(map);
    }
}
