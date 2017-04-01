package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.util.BardLogger;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.TEXT_ALIGNMENT_TEXT_START;
import static com.roplabs.bard.util.Helper.BARD_EDITOR_REQUEST_CODE;

/**
 * Created by reg on 2017-03-30.
 */
public class ChannelActivity extends BaseActivity implements SceneSelectFragment.OnSceneListener {

    private static final int MAX_SCENE_COMBO_LENGTH = 10;
    private String channelToken;
    private Channel channel;
    private ViewPager viewPager;
    private LinearLayout sceneComboContainer;
    private LinearLayout sceneComboListContainer;
    private List<Scene> sceneComboList;
    private Button clearSceneComboButton;
    private Button enterSceneComboButton;
    private ProgressBar sceneDownloadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channel = Channel.forToken(channelToken);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(channel.getName());
        title.setGravity(Gravity.LEFT);
        initPager();
        initCombo();

    }

    private void initCombo() {
        sceneComboContainer = (LinearLayout) findViewById(R.id.scene_combo_container);
        sceneComboContainer.setVisibility(View.GONE);
        sceneComboListContainer = (LinearLayout) findViewById(R.id.scene_combo_list_container);
        clearSceneComboButton = (Button) findViewById(R.id.clear_scene_combo_btn);
        enterSceneComboButton = (Button) findViewById(R.id.enter_scene_combo_btn);
        sceneDownloadProgress = (ProgressBar) findViewById(R.id.scene_download_progress);

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


                Intent intent = new Intent(getApplicationContext(), BardEditorActivity.class);
                intent.putExtra("characterToken", "");
                intent.putExtra("channelToken", channelToken);
                intent.putExtra("sceneToken", "");
                intent.putExtra("sceneTokens", TextUtils.join(",",sceneTokens));
                BardLogger.trace("[multiSceneSelect] " + sceneTokens.toString());
                startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);

            }
        });
    }

    private void initPager() {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.channel_pager);
        viewPager.setAdapter(new ChannelPagerAdapter(getSupportFragmentManager(),
                ChannelActivity.this));
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
        TabLayout tabLayout = (TabLayout) findViewById(R.id.channel_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onItemLongClick(Scene scene) {
        addComboItem(scene);
    }

    @Override
    public void onItemClick(Scene scene) {
        if (sceneComboList.isEmpty()) {
            Intent intent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("characterToken", "");
            intent.putExtra("channelToken", channelToken);
            intent.putExtra("sceneToken", scene.getToken());
            BardLogger.trace("[sceneSelect] " + scene.getToken());
            startActivityForResult(intent, BARD_EDITOR_REQUEST_CODE);
        } else {
            addComboItem(scene);
        }
    }

    private void addComboItem(Scene scene) {
        if (sceneComboList.size() >= MAX_SCENE_COMBO_LENGTH) return;
        if (sceneComboList.contains(scene)) return;

        // scene could be a remoteScene which do not contain wordList so we check local db record
        scene = Scene.forToken(scene.getToken());

        if (!sceneComboContainer.isShown()) {
            sceneComboContainer.setVisibility(View.VISIBLE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);

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

    private void removeComboItem(int sceneIndex) {
        sceneComboListContainer.removeViewAt(sceneIndex);
        sceneComboList.remove(sceneIndex);

        if (sceneComboList.isEmpty()) {
            sceneComboContainer.setVisibility(View.GONE);
        }
    }

    private void onWordListDownloadSuccess() {

    }

    private void onWordListDownloadFailure() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel, menu);
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_channel_more:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_search:
                intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
