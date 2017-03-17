package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.roplabs.bard.models.UserPack;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.adapters.CharacterListAdapter;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Set;

public class CharacterSelectActivity extends BaseActivity {
    private final int BARD_EDITOR_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("CharacterSelect onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_select);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_character);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) { actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp); }

        progressBar = (ProgressBar) findViewById(R.id.character_progress_bar);

        initEmptyState();

        RealmResults<Character> characters = UserPack.packsForUser(Setting.getUsername(this));
        displayCharacterList(characters);


        if (Setting.isLogined(this)) {
            if (characters.size() == 0) {
                progressBar.setVisibility(View.VISIBLE);
            }

            syncRemoteData();
        } else {
            if (characters.size() == 0) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }

        }
    }

    private void initEmptyState() {
        emptyStateContainer = (FrameLayout) findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("Packs - combine multiple videos");
        emptyStateDescription.setText("Visit https://bard.co/pack_builder from desktop browser while logged-in to create packs and list them here. ");

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void syncRemoteData() {
        // this fetches packs created by user

        final String username = Setting.getUsername(this);
        Call<List<Character>> call = BardClient.getAuthenticatedBardService().listCharacters(username);
        call.enqueue(new Callback<List<Character>>() {
            @Override
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                List<Character> characterList = response.body();
                Character.createOrUpdate(characterList);

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                for (Character character : characterList) {
                    UserPack userPack = UserPack.forPackTokenAndUsername(character.getToken(), username);
                    if (userPack == null) {
                        UserPack.create(realm, character.getToken(), username);
                    } else {
                        // check if timestamp is different (means need to pull in new update)
                        Character localPack = Character.forToken(userPack.getPackToken());
                        if (!localPack.getTimestamp().equals(character.getTimestamp())) {
                            // clear wordList cache
                            localPack.setIsBundleDownloaded(false);
                        }
                    }
                }
                realm.commitTransaction();

                RealmResults<Character> characters = UserPack.packsForUser(username);

                if (characters.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                }

                progressBar.setVisibility(View.GONE);
                ((CharacterListAdapter) recyclerView.getAdapter()).swap(characters);
            }

            @Override
            public void onFailure(Call<List<Character>> call, Throwable t) {
                RealmResults<Character> characters = Character.findAll();
                progressBar.setVisibility(View.GONE);
                if (characters.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Failed to load. Make sure internet is enabled", Toast.LENGTH_LONG).show();
                }
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
        BardLogger.log("displaying characters count: " + characterList.size());

        recyclerView = (RecyclerView) findViewById(R.id.index_list);
        CharacterListAdapter adapter = new CharacterListAdapter(this, characterList);
        final Context self = this;
        adapter.setOnItemClickListener(new CharacterListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Character character) {
                Intent intent = new Intent(self, BardEditorActivity.class);
                intent.putExtra("characterToken", character.getToken());
                intent.putExtra("sceneToken", "");
                BardLogger.trace("[characterSelect] " + character.getToken());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == BARD_EDITOR_REQUEST_CODE) {
            finish();
        }
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


}
