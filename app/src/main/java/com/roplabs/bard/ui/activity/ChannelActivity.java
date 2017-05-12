package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelPagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.fragment.ChannelFeedFragment;
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
import static com.roplabs.bard.util.Helper.SCENE_SELECT_REQUEST_CODE;

/**
 * Created by reg on 2017-03-30.
 */
public class ChannelActivity extends BaseActivity implements SceneSelectFragment.OnSceneListener, ChannelFeedFragment.OnChannelFeedListener, PopupMenu.OnMenuItemClickListener {

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
    private PopupMenu moreMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channel = Channel.forToken(channelToken);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);

        if (channel.getMode().equals("pair")) {
            title.setText(channel.getReceiver());
        } else {
            title.setText(channel.getName());
        }

        title.setGravity(Gravity.CENTER_HORIZONTAL);
        initChannel();
//        initCombo();

    }

    @Override
    public void onCreatePostClicked() {
        Intent intent = new Intent(this, SceneSelectActivity.class);
        intent.putExtra("mode", "channel");
        intent.putExtra("channelToken", channelToken);

        startActivityForResult(intent, SCENE_SELECT_REQUEST_CODE);

        // go to create tab
//        bottomNavigation.findViewById(R.id.action_create).performClick();
    }

    @Override
    public void onBackPressed() {
        finish();
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

    private void initChannel() {
        setFragment(ChannelFeedFragment.newInstance(channelToken));
    }

    protected void setFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.channel_fragment_container, fragment);
        fragmentTransaction.commit();
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
        if (channel.getMode().equals("group")) {
            getMenuInflater().inflate(R.menu.menu_channel, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                // click on 'up' button in the action bar, handle it here
                finish();
                return true;
            case R.id.menu_item_channel_more:
                onChannelMoreButtonClick(findViewById(R.id.menu_item_channel_more));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onChannelMoreButtonClick(View view) {
        moreMenu = new PopupMenu(this, view);
        moreMenu.setOnMenuItemClickListener(this);
        moreMenu.inflate(R.menu.menu_create_upload_video_more);
        moreMenu.inflate(R.menu.menu_create_rate_app_more);
        moreMenu.show();
    }

    private void leaveChannel(String channelToken) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userChannelRef = database.getReference("users/" + Setting.getUsername(ClientApp.getContext()) + "/channels/" + channelToken);
        userChannelRef.removeValue();

        DatabaseReference channelMemberRef = database.getReference("channels/" + channelToken + "/participants/" + Setting.getUsername(ClientApp.getContext()));
        channelMemberRef.removeValue();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Context self = this;
        String url;
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_channel_details:
                intent = new Intent(this, ChannelDetailsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_add_channel_member:
                intent = new Intent(this, ChannelMemberInviteActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_channel_leave:
                return true;
            default:
                return false;
        }
    }
}
