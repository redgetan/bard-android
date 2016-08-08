package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.adapters.CharacterListAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class CharacterSelectActivity extends BaseActivity {
    private final int NUM_GRID_COLUMNS = 2;
    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER = 4;

    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        RealmResults<Character> characterResults = Character.findAll();
        displayCharacterList(characterResults);
        syncRemoteData();

        initNavigationViewDrawer();
    }

    private void syncRemoteData() {
        Call<List<Character>> call = BardClient.getBardService().listCharacters();
        call.enqueue(new Callback<List<Character>>() {
            @Override
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                List<Character> characterList = response.body();
                Character.copyToRealmOrUpdate(characterList);
                ((CharacterListAdapter) recyclerView.getAdapter()).swap(characterList);
            }

            @Override
            public void onFailure(Call<List<Character>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "failure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }


    public void displayCharacterList(List<Character> characterList) {
        recyclerView = (RecyclerView) findViewById(R.id.index_list);
        CharacterListAdapter adapter = new CharacterListAdapter(this, characterList);
        final Context self = this;
        adapter.setOnItemClickListener(new CharacterListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Character character) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("Character", character);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                        new ProfileDrawerItem().withName(Setting.getUsername(this)).withEmail(Setting.getEmail(this)) // .withIcon(getResources().getDrawable(R.drawable.profile))
                )
                .withHeightDp(150)
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.my_projects_string).withIdentifier(MY_PROJECTS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_inbox_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.about_string).withIdentifier(ABOUT_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_info_outline_black_24dp)
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
                                intent = new Intent(mContext, CharacterSelectActivity.class);
                                startActivity(intent);
                                break;
                            case MY_PROJECTS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(mContext, RepoListActivity.class);
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
