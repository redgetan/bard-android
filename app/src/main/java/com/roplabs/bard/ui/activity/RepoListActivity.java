package com.roplabs.bard.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.DividerItemDecoration;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.adapters.RepoListAdapter;
import com.roplabs.bard.util.BardLogger;

import java.util.List;

public class RepoListActivity extends BaseActivity {

    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int NEW_BARD_DRAWER_ITEM_IDENTIFIER = 4;
    public static final int PROFILE_DRAWER_ITEM_IDENTIFIER = 5;
    public static final int TELL_FRIEND_DRAWER_ITEM_IDENTIFIER = 6;
    private static final int REQUEST_WRITE_STORAGE = 1;


    private FrameLayout emptyStateContainer;
    private RepoListAdapter adapter;
    public static final String VIDEO_LOCATION_MESSAGE = "com.roplabs.bard.VIDEO_URL";
    public static final String REPO_TOKEN_MESSAGE = "com.roplabs.bard.REPO_TOKEN";
    public static final String REPO_URL_MESSAGE = "com.roplabs.bard.REPO_URL";
    private final int CHARACTER_SELECT_REQUEST_CODE = 1;
    private final int LOGIN_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_repo_container);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.bard_library);

        displayRepoList();
        initNavigationViewDrawer();
        askStoragePermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean wasEmptyBeforeButFilledNow = adapter == null && !Repo.forUsername(Setting.getUsername(this)).isEmpty();
        boolean wasNotEmptyBefore = adapter != null;

        if (wasEmptyBeforeButFilledNow) {
            displayRepoList();
        } else if (wasNotEmptyBefore) {
            adapter.notifyDataSetChanged();
        }
    }

    private void askStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//                // Show an expanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
//            }
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


    public void displayRepoList() {
        final List<Repo> repos = Repo.forUsername(Setting.getUsername(this));

        if (repos.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        final Context self = this;

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        adapter = new RepoListAdapter(this, repos);
        adapter.setOnItemClickListener(new RepoListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Repo repo) {
                Intent intent = new Intent(self, VideoPlayerActivity.class);
                intent.putExtra("title", repo.title());
                intent.putExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE, repo.getFilePath());
                intent.putExtra(RepoListActivity.REPO_URL_MESSAGE, repo.getUrl());
                intent.putExtra(RepoListActivity.REPO_TOKEN_MESSAGE, repo.getToken());
                self.startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));



        // http://stackoverflow.com/a/27037230
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_compose:
                Intent intent = new Intent(this, CharacterSelectActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == LOGIN_REQUEST_CODE) {
            initNavigationViewDrawer();
        }
    }

    private void initNavigationViewDrawer() {
        String username = Setting.getUsername(this);
        ProfileDrawerItem profileDrawerItem;

        if (username.equals("anonymous")) {
            profileDrawerItem = new ProfileDrawerItem().withName(getResources().getString(R.string.click_to_login)).withEmail(Setting.getEmail(this)); // .withIcon(getResources().getDrawable(R.drawable.profile))
        } else {
            profileDrawerItem = new ProfileDrawerItem().withName(username).withEmail(Setting.getEmail(this)); // .withIcon(getResources().getDrawable(R.drawable.profile))
        }

        final Context self = this;

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.profile_header)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(profileDrawerItem)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (!Setting.isLogined(self)) {
                            Intent intent = new Intent(self, LoginActivity.class);
                            startActivityForResult(intent, LOGIN_REQUEST_CODE);
                        }
                        return false;
                    }
                })
                .withHeightDp(150)
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.new_bard).withIdentifier(NEW_BARD_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_create_black_24dp),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.tell_friend).withIdentifier(TELL_FRIEND_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_person_add_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.settings_string).withIdentifier(PROFILE_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_settings_black_24dp)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        Intent intent;

                        switch ((int) drawerItem.getIdentifier()) {
                            case NEW_BARD_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(getApplicationContext(), CharacterSelectActivity.class);
                                startActivity(intent);
                                break;
                            case TELL_FRIEND_DRAWER_ITEM_IDENTIFIER:
                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, you should check out https://bard.co");
                                shareIntent.setType("text/plain");
                                startActivity(shareIntent);
                                break;
                            case PROFILE_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(getApplicationContext(), ProfileActivity.class);
                                startActivity(intent);
                                break;
                            case MY_PROJECTS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(getApplicationContext(), RepoListActivity.class);
                                startActivity(intent);
                                break;
                            case ABOUT_DRAWER_ITEM_IDENTIFIER:
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://bard.co"));
                                startActivity(browserIntent);
                                break;
                            default:
                                break;
                        }

                        // allows drawer to close
                        return false;
                    }
                })
                .build();

    }

}
