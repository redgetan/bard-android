package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.activity.BardEditorActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

public class SearchResultFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";

    private int mPage;

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

    private OnSearchListener searchListener;

    // Define the events that the fragment will use to communicate
    public interface OnSearchListener  {
        // This can be any number of events to be sent to the activity
        public String getSearchQuery();
    }

    public static SearchResultFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        SearchResultFragment fragment = new SearchResultFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
//        TextView textView = (TextView) view;
//        textView.setText("Fragment #" + mPage);

        recyclerView = (RecyclerView) view.findViewById(R.id.scene_list);
        progressBar = (ProgressBar) view.findViewById(R.id.scene_progress_bar);
        sceneListCache = new HashMap<String, List<Scene>>();
        sceneListCacheExpiry = Calendar.getInstance().get(Calendar.SECOND) + (60 * 60); // 1 hour

        initEmptyState(view);
        initSearch();
        initScenes();

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnSearchListener) {
            searchListener = (OnSearchListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SearchResultFragment.OnSearchListener");
        }
    }

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void initSearch() {
        lastSearch = "";
    }

    private String getSearchType() {
        if (mPage == 1) {
            return "title";
        } else if (mPage == 2) {
            return "words";
        } else {
            return "";
        }
    }

    public void performSearch(String text) {
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
        Analytics.track(ClientApp.getContext(), "search", properties);

        Map<String, String> map = new HashMap<String, String>();
        map.put("page",String.valueOf(1));
        map.put("search", text);
        map.put("type",getSearchType());
        syncRemoteData(map);

        lastSearch = text;
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
        final Context self = ClientApp.getContext();

        this.sceneList = new ArrayList<Scene>();

        // set adapter
        SceneListAdapter adapter = new SceneListAdapter(self, this.sceneList);
        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Scene scene) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("sceneToken", scene.getToken());
                BardLogger.trace("[sceneSearch] " + scene.getToken());
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

        String search = searchListener.getSearchQuery();

        if (!search.isEmpty()) {
            data.put("search", search);
            data.put("type", getSearchType());
        }

        syncRemoteData(data);
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
    public void onResume() {
        BardLogger.log("Search onResume");

        int timeNow = Calendar.getInstance().get(Calendar.SECOND);
        if (timeNow > sceneListCacheExpiry) {
            sceneListCache.clear();
        }

        super.onResume();
    }



}
