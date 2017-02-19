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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mContext = this;

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.search);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.search_result_pager);
        viewPager.setAdapter(new SearchFragmentPagerAdapter(getSupportFragmentManager(),
                SearchActivity.this));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                SearchResultFragment page = (SearchResultFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.search_result_pager + ":" + position);
                page.performSearch(searchView.getQuery().toString());
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
        getMenuInflater().inflate(R.menu.menu_search_field, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        // Expand the search view and request focus
        searchItem.expandActionView();
        searchView.requestFocus();
        final Activity self = this;
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Intent parentIntent = new Intent(getApplicationContext(), SceneSelectActivity.class);
                startActivity(parentIntent);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // perform query here
                SearchResultFragment page = (SearchResultFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.search_result_pager + ":" + viewPager.getCurrentItem());
                page.performSearch(query);


                // workaround to avoid issues with some emulators and keyboard devices firing twice if a keyboard enter is used
                // see https://code.google.com/p/android/issues/detail?id=24599
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });



        return super.onCreateOptionsMenu(menu);
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
        return searchView.getQuery().toString();
    }
}
