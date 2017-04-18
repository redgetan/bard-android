package com.roplabs.bard.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.BardCreateFragmentPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.activity.BardEditorActivity;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static com.roplabs.bard.util.Helper.BARD_EDITOR_REQUEST_CODE;

/**
 * Created by reg on 2017-04-17.
 */
public class BardCreateFragment extends Fragment {
    private static final int MAX_SCENE_COMBO_LENGTH = 10;

    private ViewPager viewPager;
    private LinearLayout sceneComboContainer;
    private LinearLayout sceneComboListContainer;
    private List<Scene> sceneComboList;
    private Button clearSceneComboButton;
    private Button enterSceneComboButton;
    private ProgressBar sceneDownloadProgress;
    private View fragmentView;

    public static BardCreateFragment newInstance() {
        Bundle args = new Bundle();
        BardCreateFragment fragment = new BardCreateFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bard_create, container, false);

        fragmentView = view;

        initPager(view);
        initCombo(view);

        return view;
    }


    private void initPager(View view) {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) view.findViewById(R.id.bard_create_pager);
        viewPager.setAdapter(new BardCreateFragmentPagerAdapter(getActivity().getSupportFragmentManager(),
                getActivity()));
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.bard_create_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void initCombo(View view) {
        sceneComboContainer = (LinearLayout) view.findViewById(R.id.scene_combo_container);
        sceneComboContainer.setVisibility(View.GONE);
        sceneComboListContainer = (LinearLayout) view.findViewById(R.id.scene_combo_list_container);
        clearSceneComboButton = (Button) view.findViewById(R.id.clear_scene_combo_btn);
        enterSceneComboButton = (Button) view.findViewById(R.id.enter_scene_combo_btn);
        sceneDownloadProgress = (ProgressBar) view.findViewById(R.id.scene_download_progress);

        sceneComboList = new ArrayList<Scene>();

        clearSceneComboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sceneComboListContainer.removeAllViews();
                sceneComboList.clear();
                sceneComboContainer.setVisibility(View.GONE);
            }
        });

        enterSceneComboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> sceneTokens = new ArrayList<String>();
                for (Scene scene : sceneComboList) {
                    if (!scene.getWordList().isEmpty()) {
                        sceneTokens.add(scene.getToken());
                    }
                }


                Intent intent = new Intent(getActivity(), BardEditorActivity.class);
                intent.putExtra("characterToken", "");
                intent.putExtra("channelToken", Configuration.mainChannelToken());
                intent.putExtra("sceneToken", "");
                intent.putExtra("sceneTokens", TextUtils.join(",",sceneTokens));
                BardLogger.trace("[multiSceneSelect] " + sceneTokens.toString());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);

            }
        });
    }

    private void addComboItem(Scene scene) {
        if (sceneComboList.size() >= MAX_SCENE_COMBO_LENGTH) return;
        if (sceneComboList.contains(scene)) return;

        // scene could be a remoteScene which do not contain wordList so we check local db record
        scene = Scene.forToken(scene.getToken());

        if (!sceneComboContainer.isShown()) {
            sceneComboContainer.setVisibility(View.VISIBLE);
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup parent = (ViewGroup) fragmentView.findViewById(android.R.id.content);

        View sceneComboItem = inflater.inflate(R.layout.scene_combo_item, parent, false);
        sceneComboListContainer.addView(sceneComboItem);
        sceneComboList.add(scene);

        ImageView thumbnail = (ImageView) sceneComboItem.findViewById(R.id.scene_combo_item_thumbnail);
        ImageButton deleteComboItemButton = (ImageButton) sceneComboItem.findViewById(R.id.scene_combo_item_delete_btn);

        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(scene.getThumbnailUrl())
                .placeholder(R.drawable.thumbnail_placeholder)
                .crossFade()
                .into(thumbnail);

        deleteComboItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View comboItem = (View) v.getParent();
                int sceneIndex = sceneComboListContainer.indexOfChild(comboItem);
                removeComboItem(sceneIndex);
            }
        });

        getWordList(scene);
    }

    private void getWordList(final Scene scene) {
        if (!scene.getWordList().isEmpty()) {
            onWordListDownloadSuccess();
            return ;
        }

        sceneDownloadProgress.setVisibility(View.VISIBLE);
        enterSceneComboButton.setEnabled(false);
        viewPager.setEnabled(false);

        Call<Scene> call = BardClient.getAuthenticatedBardService().getSceneWordList(scene.getToken());
        call.enqueue(new Callback<Scene>() {
            @Override
            public void onResponse(Call<Scene> call, Response<Scene> response) {
                sceneDownloadProgress.setVisibility(View.GONE);
                enterSceneComboButton.setEnabled(true);
                viewPager.setEnabled(true);

                Scene remoteScene = response.body();

                if (remoteScene == null) {
                    onWordListDownloadFailure();
                    return;
                }

                String wordList = remoteScene.getWordList();

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                scene.setWordList(wordList);
                realm.commitTransaction();

                if (wordList.isEmpty()) {
                    onWordListDownloadFailure();
                } else {
                    onWordListDownloadSuccess();
                }

            }

            @Override
            public void onFailure(Call<Scene> call, Throwable t) {
                sceneDownloadProgress.setVisibility(View.GONE);
                enterSceneComboButton.setEnabled(true);
                viewPager.setEnabled(true);
                onWordListDownloadFailure();
            }
        });
    }

    private void onWordListDownloadSuccess() {

    }

    private void onWordListDownloadFailure() {

    }


    private void removeComboItem(int sceneIndex) {
        sceneComboListContainer.removeViewAt(sceneIndex);
        sceneComboList.remove(sceneIndex);

        if (sceneComboList.isEmpty()) {
            sceneComboContainer.setVisibility(View.GONE);
        }
    }

    public void onItemLongClick(Scene scene) {
        addComboItem(scene);
    }

    public void onItemClick(Scene scene) {
        Intent intent = new Intent(getActivity(), BardEditorActivity.class);
        intent.putExtra("channelToken", Configuration.mainChannelToken());
        intent.putExtra("characterToken", "");
        intent.putExtra("sceneToken", scene.getToken());
        BardLogger.trace("[sceneSelect] " + scene.getToken());
        startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
    }




}
