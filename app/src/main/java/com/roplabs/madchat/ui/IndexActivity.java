package com.roplabs.madchat.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.roplabs.madchat.R;
import com.roplabs.madchat.api.MadchatClient;
import com.roplabs.madchat.events.IndexFetchEvent;
import com.roplabs.madchat.events.IndexSelectEvent;
import com.roplabs.madchat.models.Index;
import com.roplabs.madchat.models.ItemOffsetDecoration;
import com.roplabs.madchat.models.Setting;
import com.roplabs.madchat.ui.adapter.IndexListAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class IndexActivity extends BaseActivity {
    private final int NUM_GRID_COLUMNS = 2;
    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER = 4;

    private Context mContext;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        RealmResults<Index> indexResults = Index.findAll();
        displayIndexList(indexResults);
        initNavigationViewDrawer();

    }

    private void getIndexList() throws IOException {
        MadchatClient.getIndexList();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(IndexFetchEvent event) {
        if (event.error != null) {
            Toast.makeText(getApplicationContext(), event.error, Toast.LENGTH_SHORT).show();
            return;
        }

        displayIndexList(event.indexList);
    }

    @Subscribe
    public void onEvent(IndexSelectEvent event) {
        Index index = event.index;
        Setting.setCurrentIndexToken(this, index.getToken());
        Intent intent = new Intent(this, MimicActivity.class);
        intent.putExtra("indexName",index.getName());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }


    public void displayIndexList(List<Index> indexList) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.index_list);
        IndexListAdapter adapter = new IndexListAdapter(this, indexList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, NUM_GRID_COLUMNS));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    // http://developer.android.com/guide/topics/ui/menus.html
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            if (mDrawerLayout.isDrawerOpen(mDrawerLayout.getChildAt(1)))
//                mDrawerLayout.closeDrawers();
//            else {
//                mDrawerLayout.openDrawer(mDrawerLayout.getChildAt(1));
//            }
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private void initNavigationViewDrawer() {
// Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.profile_header)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName("Mike Penz").withEmail("mikepenz@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile))
                )
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.create_string).withIdentifier(CREATE_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_create_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.choose_character_string).withIdentifier(CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.abc_ic_star_black_36dp),
                        new PrimaryDrawerItem().withName(R.string.my_projects_string).withIdentifier(MY_PROJECTS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_inbox_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.about_string).withIdentifier(ABOUT_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_settings_black_24dp)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        Intent intent;

                        switch ((int) drawerItem.getIdentifier()) {
                            case CREATE_DRAWER_ITEM_IDENTIFIER:
                                Toast.makeText(getApplicationContext(),"Create",Toast.LENGTH_SHORT).show();
                                break;
                            case CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(mContext, IndexActivity.class);
                                startActivity(intent);
                                break;
                            case MY_PROJECTS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(mContext, RepoListActivity.class);
                                startActivity(intent);
                                break;
                            case ABOUT_DRAWER_ITEM_IDENTIFIER:
                                Toast.makeText(getApplicationContext(),"Settings",Toast.LENGTH_SHORT).show();
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
