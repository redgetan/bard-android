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
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.adapters.CharacterListAdapter;
import com.roplabs.bard.util.Analytics;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class CharacterSelectActivity extends BaseActivity {
    private final int NUM_GRID_COLUMNS = 2;

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_select);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.choose_character);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) { actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp); }


        Analytics.track("compose");

        RealmResults<Character> characterResults = Character.findAll();
        displayCharacterList(characterResults);
        syncRemoteData();

    }

    private void syncRemoteData() {
        Call<List<Character>> call = BardClient.getBardService().listCharacters();
        call.enqueue(new Callback<List<Character>>() {
            @Override
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                List<Character> characterList = response.body();
                Character.createOrUpdate(characterList);
                ((CharacterListAdapter) recyclerView.getAdapter()).swap(Character.findAll());
            }

            @Override
            public void onFailure(Call<List<Character>> call, Throwable t) {
                if (Character.findAll().size() == 0) {
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
        recyclerView = (RecyclerView) findViewById(R.id.index_list);
        CharacterListAdapter adapter = new CharacterListAdapter(this, characterList);
        final Context self = this;
        adapter.setOnItemClickListener(new CharacterListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Character character) {
                Intent intent = new Intent(self, SceneSelectActivity.class);
                intent.putExtra("characterToken", character.getToken());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.character_item_offset);
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


}
