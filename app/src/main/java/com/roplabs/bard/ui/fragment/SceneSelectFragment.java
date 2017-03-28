package com.roplabs.bard.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
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

    private HashMap<String, List<Scene>> sceneListCache;
    private int sceneListCacheExpiry;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public List<Scene> sceneList;
    private EndlessRecyclerViewScrollListener scrollListener;
    private String sceneType;
    private EditText searchBar;
    private Spinner searchTypeSpinner;
    private LinearLayout sceneComboContainer;
    private LinearLayout sceneComboListContainer;
    private List<Scene> sceneComboList;
    private Button clearSceneComboButton;
    private Button enterSceneComboButton;
    private ViewGroup fragmentContainer;

    private SearchListAdapter searchListAdapter;

    private OnSceneListener parentListener;

    // Define the events that the fragment will use to communicate
    public interface OnSceneListener  {
        // This can be any number of events to be sent to the activity
        public void onComboAdd(Scene scene);
    }

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
        fragmentContainer = container;
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnSceneListener) {
            parentListener = (OnSceneListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SceneSelectFragment.OnSceneListener");
        }
    }

    private void initSearch() {

        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard();
                }
            }
        });


        // handle search
        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    performSearch(searchBar.getText().toString());
                    hideKeyboard();

                    return true;
                }
                return false;
            }
        });

        searchTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch(searchBar.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
    }


    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    public void performSearch(String text) {
        hideEmptySearchMessage();

        sceneList.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        scrollListener.resetState();

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));

        if (!getSearchQuery().isEmpty()) {
            map.put("search", text);
            map.put("type",getSearchType());
        }

        map.put("category", sceneType);
        syncRemoteData(map);
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


        // track search
        if (options.get("search") != null) {
            Bundle params = new Bundle();
            params.putString("text", options.get("search"));
            params.putString("type", options.get("type"));
            Analytics.track(ClientApp.getContext(), "search", params);
        }


        // fetch remote
        progressBar.setVisibility(View.VISIBLE);

        BardLogger.log("SYNC_REMOTE_DATA..");
        Call<List<Scene>> call = BardClient.getAuthenticatedBardService().listScenes(options);
        call.enqueue(new Callback<List<Scene>>() {
            @Override
            public void onResponse(Call<List<Scene>> call, Response<List<Scene>> response) {
                progressBar.setVisibility(View.GONE);
                List<Scene> remoteSceneList = response.body();

                if (remoteSceneList == null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    emptyStateTitle.setText("Request Failed");
                    emptyStateDescription.setText("Currently unable to fetch data from server. Try again later.");
                } else if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
                    // maybe empty on server (i.e. page 2 empty), but if page 1 has results or local db has favorites, then dont say empty
                    if (remoteSceneList.isEmpty() && sceneList.isEmpty()) {
                        showEmptyBookmarkMessage();
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

            @Override
            public void onItemLongClick(View itemView, int position, Scene scene) {
                hideKeyboard();
                parentListener.onComboAdd(scene);
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

        sceneListCache.clear();
        int timeNow = Calendar.getInstance().get(Calendar.SECOND);
        if (timeNow > sceneListCacheExpiry) {
        }

        // refresh data
        performSearch(getSearchQuery());

        super.onResume();
    }

    private void showEmptyBookmarkMessage() {
        emptyStateContainer.setVisibility(View.VISIBLE);
        emptyStateTitle.setText("No results");
    }

}
