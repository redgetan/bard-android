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
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
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

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private final static int BARD_EDITOR_REQUEST_CODE = 1;
    private String lastSearch;
    private SearchView searchView;
    private ViewPager viewPager;
    private EditText searchBar;

    private ImageView cancelSearchBtn;
    private ImageView clearSearchBtn;


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


        initSearch();

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
                Intent intent = new Intent(ClientApp.getContext(), SceneSelectActivity.class);
                startActivity(intent);
            }
        });

        clearSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

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
}
