package com.roplabs.bard.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.roplabs.bard.ui.activity.UploadVideoActivity;
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
    public static final String SCENE_TYPE = "SCENE_TYPE";
    public static final String CHANNEL_TOKEN = "CHANNEL_TOKEN";

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
    private String channelToken;
    private EditText searchBar;
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
        public void onItemClick(Scene scene);
        public void onItemLongClick(Scene scene);
    }

    public static SceneSelectFragment newInstance(String sceneType) {
        Bundle args = new Bundle();
        args.putString(SCENE_TYPE, sceneType);
        SceneSelectFragment fragment = new SceneSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static SceneSelectFragment newInstance(String sceneType, String channelToken) {
        Bundle args = new Bundle();
        args.putString(CHANNEL_TOKEN, channelToken);
        args.putString(SCENE_TYPE, sceneType);
        SceneSelectFragment fragment = new SceneSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sceneType = getArguments().getString(SCENE_TYPE);
        channelToken = getArguments().getString(CHANNEL_TOKEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentContainer = container;
        View view = inflater.inflate(R.layout.fragment_scene_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.scene_list);
        progressBar = (ProgressBar) view.findViewById(R.id.scene_progress_bar);
        searchBar = (EditText) view.findViewById(R.id.scene_search_input);

        if (sceneListCache == null) {
            sceneListCache = new HashMap<String, List<Scene>>();
            sceneListCacheExpiry = Calendar.getInstance().get(Calendar.SECOND) + (60 * 60); // 1 hour
        }

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


        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(searchBar.getText().toString());
                    hideKeyboard();
                    return true;
                }
                return false;
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

        if (sceneType.equals("uploads")) {
            emptyStateContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (emptyStateDescription.getText().toString().contains("Upload an existing")) {
                        Intent intent = new Intent(getActivity(), UploadVideoActivity.class);
                        startActivity(intent);
                    } else {
                        // tap to refresh
                        hideEmptySearchMessage();
                        getScenesNextPage(1);
                    }
                }
            });
        } else {
            emptyStateContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // tap to refresh
                    hideEmptySearchMessage();
                    getScenesNextPage(1);
                }
            });
        }
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

        if (channelToken != null) {
            map.put("channel_token", channelToken);
        }
        syncRemoteData(map);
    }

    private String getSearchType() {
        return "words";
    }

    private void hideEmptySearchMessage() {
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void displayEmptySearchMessage() {
        if (sceneType.equals("uploads")) {
            emptyStateTitle.setText("No results");
            emptyStateDescription.setText(buildMissingSearchMessage());
        } else {
            emptyStateTitle.setText("No results found");
            emptyStateDescription.setText("Try another search or Upload an existing video");
        }
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

        options.put("locale",Locale.getDefault().getLanguage());


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
                    emptyStateDescription.setText("Tap to refresh.");
                } else {
                    emptyStateTitle.setText(R.string.no_network_connection);
                    emptyStateDescription.setText("Tap to refresh");
                }

//                if (Scene.findAll().size() == 0) {
//                    Toast.makeText(getApplicationContext(), "Failed to load. Make sure internet is enabled", Toast.LENGTH_SHORT).show();
//                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        BardLogger.log("destroyview: sceneselectfragment " + channelToken + ":" + sceneType );
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        BardLogger.log("destroy: sceneselectfragment" + channelToken + ":" + sceneType);
        super.onDestroy();
    }

    private void syncRemoteFavoritesToLocal(List<Scene> remoteSceneList) {
        Favorite.createOrUpdate(remoteSceneList);
    }

    private void populateScenes(List<Scene> remoteSceneList, Map<String, String> options) {
        for (Scene scene : remoteSceneList) {
            if (scene.getToken() == null) continue;

            Scene localScene = Scene.forToken(scene.getToken());
            if (localScene == null) {
                // create if scene doesnt exist yet
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                Scene.create(realm, scene.getToken(),"",scene.getName(),scene.getThumbnailUrl(), scene.getOwner(), scene.getLabeler(), scene.getTagList(), scene.getDuration());
                realm.commitTransaction();
            } else {
                Scene.setOwnerLabelerTagList(localScene, scene);
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

        if (this.sceneList == null) {
            this.sceneList = new ArrayList<Scene>();
        }

        // set adapter
        SceneListAdapter adapter = new SceneListAdapter(self, this.sceneList);
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                parentListener.onItemClick(scene);
            }

            @Override
            public void onItemLongClick(View itemView, int position, Scene scene) {
                hideKeyboard();
                parentListener.onItemLongClick(scene);
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
                if (totalItemsCount < 12) {

                } else {
                    BardLogger.log("LOAD_MORE: " + page);
                    getScenesNextPage(page);
                }
            }
        };

//        if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
//            sceneListCache.clear();
//            loadBookmarks();
//        } else {
            recyclerView.addOnScrollListener(scrollListener);
//        }
    }

    private void loadBookmarks() {
        RealmResults<Scene> bookmarks = Scene.favoritesForUsername(Setting.getUsername(ClientApp.getContext()));
        Map<String, String> options = new HashMap<String, String>();
        if (bookmarks.isEmpty()) {
            showEmptyBookmarkMessage();
        } else {
            hideEmptySearchMessage();
            populateScenes(bookmarks, options);
        }
    }

    private void getScenesNextPage(int page) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("page", String.valueOf(page));
        data.put("category", sceneType);
        if (channelToken != null) {
            data.put("channel_token", channelToken);
        }

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

    private SpannableStringBuilder buildMissingSearchMessage() {
        String first = "";
        String last = "Upload an existing video";
        final SpannableStringBuilder sb = new SpannableStringBuilder(first + last);

        // Span to set text color to some RGB value
        final ForegroundColorSpan fcs = new ForegroundColorSpan(R.color.md_blue_500);

        // Span to make text bold
        final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

        sb.setSpan(new UnderlineSpan(), first.length(), first.length() + last.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        sb.setSpan(fcs, first.length(), first.length() + last.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        sb.setSpan(bss, first.length(), first.length() + last.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return sb;
    }

    @Override
    public void onResume() {
        BardLogger.log("SceneSelectFragment onResume");

        int timeNow = Calendar.getInstance().get(Calendar.SECOND);
        if (timeNow > sceneListCacheExpiry) {
            sceneListCache.clear();
        }

//        if (sceneType.equals(Helper.FAVORITES_SCENE_TYPE)) {
//            loadBookmarks();
//        } else {
            // refresh data
            performSearch(getSearchQuery());
//        }

        super.onResume();
    }

    private void showEmptyBookmarkMessage() {
        emptyStateContainer.setVisibility(View.VISIBLE);
        emptyStateTitle.setText("No results");
        emptyStateDescription.setText("");
    }

}
