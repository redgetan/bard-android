package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.roplabs.bard.R;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.DividerItemDecoration;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.adapters.RepoListAdapter;

import java.util.List;

public class RepoListActivity extends BaseActivity {

    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int NEW_BARD_DRAWER_ITEM_IDENTIFIER = 4;
    public static final int PROFILE_DRAWER_ITEM_IDENTIFIER = 5;
    public static final int TELL_FRIEND_DRAWER_ITEM_IDENTIFIER = 6;

    private FrameLayout emptyStateContainer;
    public static final String VIDEO_LOCATION_MESSAGE = "com.roplabs.bard.VIDEO_URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_repo_container);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.bard_library);

        displayRepoList();
        initNavigationViewDrawer();
    }

    public void displayRepoList() {
        final List<Repo> repos = Repo.findAll();

        if (repos.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.user_projects);
        RepoListAdapter adapter = new RepoListAdapter(this, repos);
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

    private void initNavigationViewDrawer() {

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.profile_header)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName(Setting.getUsername(this)).withEmail(Setting.getEmail(this)) // .withIcon(getResources().getDrawable(R.drawable.profile))
                )
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
