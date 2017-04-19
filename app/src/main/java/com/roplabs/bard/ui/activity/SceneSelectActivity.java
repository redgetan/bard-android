package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneSelectFragmentPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.fragment.BardCreateFragment;
import com.roplabs.bard.ui.fragment.ChannelFeedFragment;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.ui.widget.NonSwipingViewPager;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

import static com.roplabs.bard.util.Helper.*;

public class SceneSelectActivity extends BaseActivity implements ChannelFeedFragment.OnChannelFeedListener, SceneSelectFragment.OnSceneListener  {
    private static final int MAX_SCENE_COMBO_LENGTH = 10;
    private Context mContext;
    private DrawerLayout mDrawerLayout;

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private NonSwipingViewPager viewPager;

    private BottomNavigationView bottomNavigation;
    private String channelToken;
    private FragmentManager fragmentManager;
    private Fragment fragment;
    private Map<Integer, Fragment> fragmentCache;
    private Menu activityMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        mContext = this;

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.app_name);
//        title.setTextSize(24);

        channelToken = Configuration.mainChannelToken();
        Locale.getDefault().getLanguage();

        initBottomNavigation();

        postInitSetup();
    }

    public void postInitSetup() {
        deepLinkNavigate();

//        Helper.initNavigationViewDrawer(this, toolbar);
        Helper.askStoragePermission(this);
    }

    private void initBottomNavigation() {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        fragmentCache = new HashMap<Integer, Fragment>();

        fragmentManager = getSupportFragmentManager();
        viewPager = (NonSwipingViewPager) findViewById(R.id.scene_select_pager);
        viewPager.setPagingEnabled(false);
        viewPager.setAdapter(new SceneSelectFragmentPagerAdapter(getSupportFragmentManager(), this, Configuration.mainChannelToken()));
        viewPager.setOffscreenPageLimit(2);

        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id){
                    case R.id.action_channels:
                        viewPager.setCurrentItem(0);
                        showChannelToolbar();

                        break;
                    case R.id.action_create:
                        viewPager.setCurrentItem(1);
                        showCreateToolbar();

                        break;
                    case R.id.action_profile:
                        viewPager.setCurrentItem(2);
                        showProfileToolbar();

                        break;
                }

                return true;
            }
        });

        // default first tab is "create"
        bottomNavigation.findViewById(R.id.action_create).performClick();

    }


    private void showChannelToolbar() {
        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_upload_video);
        menuItem.setVisible(false);
        menuItem = activityMenu.findItem(R.id.menu_item_rate_app);
        menuItem.setVisible(false);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Feed");
    }

    private void showCreateToolbar() {
        if (activityMenu == null) return;

        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(true);

        menuItem = activityMenu.findItem(R.id.menu_item_upload_video);
        menuItem.setVisible(true);
        menuItem = activityMenu.findItem(R.id.menu_item_rate_app);
        menuItem.setVisible(true);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Bard");
    }

    private void showProfileToolbar() {
        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_upload_video);
        menuItem.setVisible(false);
        menuItem = activityMenu.findItem(R.id.menu_item_rate_app);
        menuItem.setVisible(false);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Profile");
    }

    private void deepLinkNavigate() {
        Intent intent = getIntent();
        String sceneToken;
        String characterToken;

        if ((sceneToken = intent.getStringExtra("sceneTokenEditorDeepLink")) != null) {
            Intent newIntent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("channelToken", channelToken);
            newIntent.putExtra("characterToken", "");
            newIntent.putExtra("sceneToken", sceneToken);
            BardLogger.trace("[sceneDeepLink] " + sceneToken);
            startActivityForResult(newIntent, BARD_EDITOR_REQUEST_CODE);
        } else if ((characterToken = intent.getStringExtra("packTokenEditorDeepLink")) != null) {
            Intent newIntent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("channelToken", channelToken);
            newIntent.putExtra("characterToken", characterToken);
            newIntent.putExtra("sceneToken", "");
            BardLogger.trace("[packDeepLink] " + characterToken);
            startActivityForResult(newIntent, BARD_EDITOR_REQUEST_CODE);
        }
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
        activityMenu = menu;

        getMenuInflater().inflate(R.menu.menu_search, menu);
        getMenuInflater().inflate(R.menu.menu_create_more, menu);
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_search:
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra("channelToken", channelToken);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
                return true;
            case R.id.menu_item_upload_video:
                intent = new Intent(this, UploadVideoActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_rate_app:
                Helper.openInAppStore(ClientApp.getContext());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        BardLogger.log("Scene Select onResume");

//        Helper.initNavigationViewDrawer(this, toolbar);
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
        if (resultCode == RESULT_OK &&
                (requestCode == BARD_EDITOR_REQUEST_CODE || requestCode == SEARCH_REQUEST_CODE) ) {
            boolean shouldBackToChannel = data.getBooleanExtra("backToChannel", false);
            if (shouldBackToChannel) {
                // navigate to feed tab
                bottomNavigation.findViewById(R.id.action_channels).performClick();

            }
        } else if (resultCode == RESULT_OK && requestCode == LOGIN_REQUEST_CODE) {
        }
    }

    @Override
    public void onItemLongClick(Scene scene) {
        BardCreateFragment bardCreateFragment = (BardCreateFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.scene_select_pager + ":" + SceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition());
        bardCreateFragment.addComboItem(scene);
    }

    @Override
    public void onItemClick(Scene scene) {
        BardCreateFragment bardCreateFragment = (BardCreateFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.scene_select_pager + ":" + SceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition());
        bardCreateFragment.openScene(scene);
    }

    @Override
    public void onCreatePostClicked() {
        // go to create tab
        bottomNavigation.findViewById(R.id.action_create).performClick();
    }
}
