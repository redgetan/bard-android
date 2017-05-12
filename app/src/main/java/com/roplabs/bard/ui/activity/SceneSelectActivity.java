package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.Layout;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneSelectFragmentPagerAdapter;
import com.roplabs.bard.adapters.SimpleSceneSelectFragmentPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.fragment.BardCreateFragment;
import com.roplabs.bard.ui.fragment.ChannelFeedFragment;
import com.roplabs.bard.ui.fragment.ProfileFragment;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.ui.widget.CustomDialog;
import com.roplabs.bard.ui.widget.NonSwipingViewPager;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

import static com.roplabs.bard.util.Helper.*;

public class SceneSelectActivity extends BaseActivity implements ChannelFeedFragment.OnChannelFeedListener, SceneSelectFragment.OnSceneListener, PopupMenu.OnMenuItemClickListener {
    private static final int MAX_SCENE_COMBO_LENGTH = 10;
    private Context mContext;
    private DrawerLayout mDrawerLayout;

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private NonSwipingViewPager viewPager;

    private BottomNavigationView bottomNavigation;
    private BottomNavigationView simpleBottomNavigation;
    private String channelToken;
    private Fragment fragment;
    private Menu activityMenu;
    private PopupMenu moreMenu;
    private String sceneSelectMode;
    private CustomDialog loginDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        mContext = this;

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
//        title.setTextSize(24);

        Intent intent = getIntent();
        sceneSelectMode = intent.getStringExtra("mode");

        if (sceneSelectMode != null && sceneSelectMode.equals("channel")) {
            channelToken = intent.getStringExtra("channelToken");
            title.setText("New Post");
        } else {
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            channelToken = Configuration.mainChannelToken();
            title.setText(R.string.app_name);
        }

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
        simpleBottomNavigation = (BottomNavigationView) findViewById(R.id.simple_bottom_navigation);

        viewPager = (NonSwipingViewPager) findViewById(R.id.scene_select_pager);
        viewPager.setPagingEnabled(false);
        viewPager.setOffscreenPageLimit(2);

        if (sceneSelectMode.equals("channel")) {
            bottomNavigation.setVisibility(View.GONE);
            simpleBottomNavigation.setVisibility(View.VISIBLE);
            viewPager.setAdapter(new SimpleSceneSelectFragmentPagerAdapter(getSupportFragmentManager(), this, channelToken));
        } else {
            simpleBottomNavigation.setVisibility(View.GONE);
            viewPager.setAdapter(new SceneSelectFragmentPagerAdapter(getSupportFragmentManager(), this, Configuration.mainChannelToken()));

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
                            refreshUserProfile();

                            break;
                    }

                    return true;
                }
            });

            // default first tab is "create"
            bottomNavigation.findViewById(R.id.action_create).performClick();
        }


    }


    private void showChannelToolbar() {
        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_create_more);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_channel_add);
        menuItem.setVisible(true);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Chat");
    }

    private void showCreateToolbar() {
        if (activityMenu == null) return;

        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(true);

        menuItem = activityMenu.findItem(R.id.menu_item_create_more);
        menuItem.setVisible(true);

        menuItem = activityMenu.findItem(R.id.menu_item_channel_add);
        menuItem.setVisible(false);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Bard");
    }

    private void showProfileToolbar() {
        MenuItem menuItem = activityMenu.findItem(R.id.menu_item_search);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_create_more);
        menuItem.setVisible(false);

        menuItem = activityMenu.findItem(R.id.menu_item_channel_add);
        menuItem.setVisible(false);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Profile");
    }

    private void deepLinkNavigate() {
        Intent intent = getIntent();
        String sceneToken;
        String characterToken;
        String channelTokenFromDeepLink;

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
        } else if ((channelTokenFromDeepLink = intent.getStringExtra("channelTokenDeepLink")) != null) {
            joinChannel(channelTokenFromDeepLink);
        }
    }

    private void joinChannel(String channelToken) {
        if (Setting.isLogined(this)) {
            DatabaseReference channelParticipantsRef = FirebaseDatabase.getInstance().getReference("channels/" + channelToken + "/participants/" + Setting.getUsername(ClientApp.getContext()));
            channelParticipantsRef.setValue(true);

            DatabaseReference userChannelsRef = FirebaseDatabase.getInstance().getReference("users/" + Setting.getUsername(ClientApp.getContext()) + "/channels/" + channelToken);
            userChannelsRef.setValue(true);
        } else {
            loginDialog = new CustomDialog(this, "You must login to join group chat");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
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

        getMenuInflater().inflate(R.menu.menu_channel_list, menu);
        getMenuInflater().inflate(R.menu.menu_search, menu);

        if (sceneSelectMode != null && sceneSelectMode.equals("channel")) {

        } else {
            getMenuInflater().inflate(R.menu.menu_create_more, menu);
        }

        activityMenu.findItem(R.id.menu_item_channel_add).setVisible(false);
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_item_channel_add:
                if (Setting.isLogined(this)) {
                    intent = new Intent(this, MessageNewActivity.class);
                    startActivityForResult(intent, NEW_MESSAGE_REQUEST_CODE);
                } else {
                    loginDialog = new CustomDialog(this, "You must login to chat with other users");
                    loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    loginDialog.show();
                }
                return true;
            case R.id.menu_item_settings:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_search:
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra("channelToken", channelToken);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
                return true;
            case R.id.menu_item_create_more:
                onSceneSelectMoreButtonClick(findViewById(R.id.menu_item_create_more));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSceneSelectMoreButtonClick(View view) {
        moreMenu = new PopupMenu(this, view);
        moreMenu.setOnMenuItemClickListener(this);
        moreMenu.inflate(R.menu.menu_create_upload_video_more);
        moreMenu.inflate(R.menu.menu_create_rate_app_more);
        moreMenu.show();
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
        if (sceneSelectMode.equals("channel")) {
            finish();
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        BardLogger.log("sceneselect is destroyed");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == BARD_EDITOR_REQUEST_CODE || requestCode == SEARCH_REQUEST_CODE)) {
            boolean shouldBackToChannel = data.getBooleanExtra("backToChannel", false);
            if (shouldBackToChannel) {
                // navigate to feed tab
                if (sceneSelectMode.equals("channel")) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    bottomNavigation.findViewById(R.id.action_channels).performClick();
                }

            }
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.LOGIN_REQUEST_CODE) {
            Intent intent = new Intent(ClientApp.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.SIGNUP_REQUEST_CODE) {
            Intent intent = new Intent(ClientApp.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    public void onItemLongClick(Scene scene) {
        int fragmentPosition;
        if (sceneSelectMode != null && sceneSelectMode.equals("channel")) {
            fragmentPosition = SimpleSceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition();
        } else {
            fragmentPosition = SceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition();
        }
        BardCreateFragment bardCreateFragment = (BardCreateFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.scene_select_pager + ":" + fragmentPosition);
        bardCreateFragment.addComboItem(scene);
    }

    @Override
    public void onItemClick(Scene scene) {
        int fragmentPosition;
        if (sceneSelectMode != null && sceneSelectMode.equals("channel")) {
            fragmentPosition = SimpleSceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition();
        } else {
            fragmentPosition = SceneSelectFragmentPagerAdapter.getBardCreateFragmentPosition();
        }
        BardCreateFragment bardCreateFragment = (BardCreateFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.scene_select_pager + ":" + fragmentPosition);
        bardCreateFragment.openScene(scene);
    }

    private void refreshUserProfile() {
        ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.scene_select_pager + ":" + SceneSelectFragmentPagerAdapter.getProfileFragmentPosition());
        profileFragment.refreshUserProfile();
    }

    @Override
    public void onCreatePostClicked() {
        // go to create tab
        bottomNavigation.findViewById(R.id.action_create).performClick();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Context self = this;
        String url;
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_upload_video:
                intent = new Intent(this, UploadVideoActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_rate_app:
                Helper.openInAppStore(this);
                return true;
            default:
                return false;
        }
    }
}
