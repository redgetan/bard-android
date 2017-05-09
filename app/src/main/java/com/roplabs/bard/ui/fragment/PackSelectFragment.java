package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.CharacterListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.UserPack;
import com.roplabs.bard.ui.activity.BardEditorActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

import static com.roplabs.bard.ui.fragment.SceneSelectFragment.SCENE_TYPE;
import static com.roplabs.bard.util.Helper.BARD_EDITOR_REQUEST_CODE;

public class PackSelectFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    private OnPackListener parentListener;


    public interface OnPackListener {
        public void onItemClick(Character pack);
    }

    public static PackSelectFragment newInstance() {
        Bundle args = new Bundle();
        PackSelectFragment fragment = new PackSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pack_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.index_list);
        progressBar = (ProgressBar) view.findViewById(R.id.pack_progress_bar);


        initEmptyState(view);
        initPackList();

        return view;
    }

    private void initPackList() {
        RealmResults<Character> characters = UserPack.packsForUser(Setting.getUsername(ClientApp.getContext()));
        displayCharacterList(characters);


        if (Setting.isLogined(ClientApp.getContext())) {
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

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        emptyStateTitle.setText("");
        emptyStateDescription.setText("You can assemble words from multiple videos at the same time by doing a 'long tap' on a video instead of a single tap. You can also save your favorite combinations into a pack ");

        emptyStateContainer.setVisibility(View.GONE);
    }

    private void syncRemoteData() {
        // this fetches packs created by user

        final String username = Setting.getUsername(ClientApp.getContext());
        Call<List<Character>> call = BardClient.getAuthenticatedBardService().listCharacters(username);
        call.enqueue(new Callback<List<Character>>() {
            @Override
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                List<Character> characterList = response.body();

                if (characterList != null) {
                    Character.createOrUpdate(characterList);
                }

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
            }
        });
    }

    public void displayCharacterList(List<Character> characterList) {
        BardLogger.log("displaying characters count: " + characterList.size());

        CharacterListAdapter adapter = new CharacterListAdapter(getActivity(), characterList);
        final Context self = getActivity();
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    public void onResume() {
        // fetch from local db
        RealmResults<Character> characters = UserPack.packsForUser(Setting.getUsername(ClientApp.getContext()));
        ((CharacterListAdapter) recyclerView.getAdapter()).swap(characters);
        recyclerView.getAdapter().notifyDataSetChanged();

        if (characters.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
        }

        super.onResume();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        if (context instanceof OnPackListener) {
//            parentListener = (OnPackListener) context;
//        } else {
//            throw new ClassCastException(context.toString()
//                    + " must implement SceneSelectFragment.OnSceneListener");
//        }
    }
}
