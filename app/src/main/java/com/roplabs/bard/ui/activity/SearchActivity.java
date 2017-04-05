package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.adapters.SearchFragmentPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.fragment.SearchResultFragment;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.support.v7.widget.SearchView;


import java.util.*;

public class SearchActivity extends BaseActivity implements SearchResultFragment.OnSearchListener {
    private Context mContext;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public List<Scene> sceneList;
    private EndlessRecyclerViewScrollListener scrollListener;

    private static final int MAX_SCENE_COMBO_LENGTH = 10;
    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private final static int BARD_EDITOR_REQUEST_CODE = 1;
    private String lastSearch;
    private SearchView searchView;
    private ViewPager viewPager;
    private EditText searchBar;

    private ImageView cancelSearchBtn;
    private ImageView clearSearchBtn;
    private LinearLayout sceneComboContainer;
    private LinearLayout sceneComboListContainer;
    private List<Scene> sceneComboList;
    private Button clearSceneComboButton;
    private Button enterSceneComboButton;
    private ProgressBar sceneDownloadProgress;
    private String channelToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mContext = this;

        searchBar = (EditText) toolbar.findViewById(R.id.video_search_input);
        searchBar.requestFocus();
        clearSearchBtn = (ImageView) toolbar.findViewById(R.id.clear_search_btn);
        cancelSearchBtn = (ImageView) toolbar.findViewById(R.id.cancel_search_btn);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");

        initSearch();
        initCombo();

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.search_result_pager);
        viewPager.setAdapter(new SearchFragmentPagerAdapter(getSupportFragmentManager(),
                this));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                SearchResultFragment page = (SearchResultFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.search_result_pager + ":" + position);
                page.performSearch(searchBar.getText().toString());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.search_result_tabs);
        tabLayout.setupWithViewPager(viewPager);

        // show keyboard
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    private void initSearch() {
        lastSearch = "";

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    BardLogger.log("user clicked search icon ...");
                    SearchResultFragment page = (SearchResultFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.search_result_pager + ":" + viewPager.getCurrentItem());
                    page.performSearch(v.getText().toString());
                    return true;
                }
                return false;
            }
        });

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    SearchResultFragment page = (SearchResultFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.search_result_pager + ":" + viewPager.getCurrentItem());
                    page.performSearch(searchBar.getText().toString());
                    return true;
                }
                return false;
            }
        });

        cancelSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            finish();
            }
        });

        clearSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

    }

    private void initCombo() {
        sceneComboContainer = (LinearLayout) findViewById(R.id.scene_combo_container);
        sceneComboContainer.setVisibility(View.GONE);
        sceneComboListContainer = (LinearLayout) findViewById(R.id.scene_combo_list_container);
        clearSceneComboButton = (Button) findViewById(R.id.clear_scene_combo_btn);
        enterSceneComboButton = (Button) findViewById(R.id.enter_scene_combo_btn);
        sceneDownloadProgress = (ProgressBar) findViewById(R.id.scene_download_progress);

        sceneComboList = new ArrayList<Scene>();

        clearSceneComboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sceneComboListContainer.removeAllViews();
                sceneComboList.clear();
                sceneComboContainer.setVisibility(View.GONE);
            }
        });

        enterSceneComboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> sceneTokens = new ArrayList<String>();
                for (Scene scene : sceneComboList) {
                    if (!scene.getWordList().isEmpty()) {
                        sceneTokens.add(scene.getToken());
                    }
                }


                Intent intent = new Intent(getApplicationContext(), BardEditorActivity.class);
                intent.putExtra("characterToken", "");
                intent.putExtra("channelToken", channelToken);
                intent.putExtra("sceneToken", "");
                intent.putExtra("sceneTokens", TextUtils.join(",",sceneTokens));
                BardLogger.trace("[multiSceneSelect] " + sceneTokens.toString());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);

            }
        });
    }

    @Override
    protected void onDestroy() {
        BardLogger.log("search activity destroy");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public String getSearchQuery() {
        return searchBar.getText().toString();
    }

    @Override
    public void onItemClick(Scene scene) {
        Intent intent = new Intent(this, BardEditorActivity.class);
        intent.putExtra("characterToken", "");
        intent.putExtra("sceneToken", scene.getToken());
        BardLogger.trace("[sceneSearch] " + scene.getToken());
        startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
    }

    @Override
    public void onItemLongClick(Scene scene) {
        hideKeyboard();
        addComboItem(scene);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
    }

    private void addComboItem(Scene scene) {
        if (sceneComboList.size() >= MAX_SCENE_COMBO_LENGTH) return;
        if (sceneComboList.contains(scene)) return;

        // scene could be a remoteScene which do not contain wordList so we check local db record
        scene = Scene.forToken(scene.getToken());

        if (!sceneComboContainer.isShown()) {
            sceneComboContainer.setVisibility(View.VISIBLE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);

        View sceneComboItem = inflater.inflate(R.layout.scene_combo_item, parent, false);
        sceneComboListContainer.addView(sceneComboItem);
        sceneComboList.add(scene);

        ImageView thumbnail = (ImageView) sceneComboItem.findViewById(R.id.scene_combo_item_thumbnail);
        ImageButton deleteComboItemButton = (ImageButton) sceneComboItem.findViewById(R.id.scene_combo_item_delete_btn);

        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(scene.getThumbnailUrl())
                .placeholder(R.drawable.thumbnail_placeholder)
                .crossFade()
                .into(thumbnail);

        deleteComboItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View comboItem = (View) v.getParent();
                int sceneIndex = sceneComboListContainer.indexOfChild(comboItem);
                removeComboItem(sceneIndex);
            }
        });

        getWordList(scene);
    }

    private void onWordListDownloadSuccess() {

    }

    private void onWordListDownloadFailure() {

    }

    private void getWordList(final Scene scene) {
        if (!scene.getWordList().isEmpty()) {
            onWordListDownloadSuccess();
            return ;
        }

        sceneDownloadProgress.setVisibility(View.VISIBLE);
        enterSceneComboButton.setEnabled(false);
        viewPager.setEnabled(false);

        Call<Scene> call = BardClient.getAuthenticatedBardService().getSceneWordList(scene.getToken());
        call.enqueue(new Callback<Scene>() {
            @Override
            public void onResponse(Call<Scene> call, Response<Scene> response) {
                sceneDownloadProgress.setVisibility(View.GONE);
                enterSceneComboButton.setEnabled(true);
                viewPager.setEnabled(true);

                Scene remoteScene = response.body();

                if (remoteScene == null) {
                    onWordListDownloadFailure();
                    return;
                }

                String wordList = remoteScene.getWordList();

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                scene.setWordList(wordList);
                realm.commitTransaction();

                if (wordList.isEmpty()) {
                    onWordListDownloadFailure();
                } else {
                    onWordListDownloadSuccess();
                }

            }

            @Override
            public void onFailure(Call<Scene> call, Throwable t) {
                sceneDownloadProgress.setVisibility(View.GONE);
                enterSceneComboButton.setEnabled(true);
                viewPager.setEnabled(true);
                onWordListDownloadFailure();
            }
        });
    }

    private void removeComboItem(int sceneIndex) {
        sceneComboListContainer.removeViewAt(sceneIndex);
        sceneComboList.remove(sceneIndex);

        if (sceneComboList.isEmpty()) {
            sceneComboContainer.setVisibility(View.GONE);
        }
    }
}
