package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SceneListAdapter;
import com.roplabs.bard.adapters.SceneSelectFragmentPagerAdapter;
import com.roplabs.bard.adapters.SmartFragmentStatePagerAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static com.roplabs.bard.util.Helper.LOGIN_REQUEST_CODE;
import static com.roplabs.bard.util.Helper.REQUEST_WRITE_STORAGE;
import static com.roplabs.bard.util.Helper.SEARCH_REQUEST_CODE;

public class SceneSelectActivity extends BaseActivity implements SceneSelectFragment.OnSceneListener {
    private static final int MAX_SCENE_COMBO_LENGTH = 10;
    private Context mContext;
    private DrawerLayout mDrawerLayout;

    private final static int LEFT_DRAWABLE_INDEX = 0;
    private final static int RIGHT_DRAWABLE_INDEX = 2;
    private final static int BARD_EDITOR_REQUEST_CODE = 1;
    private ViewPager viewPager;

    private LinearLayout sceneComboContainer;
    private LinearLayout sceneComboListContainer;
    private List<Scene> sceneComboList;
    private Button clearSceneComboButton;
    private Button enterSceneComboButton;
    private ProgressBar sceneDownloadProgress;
    private String channelToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Scene Select onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_select);

        mContext = this;

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.app_name);
        title.setTextSize(24);

        channelToken = Configuration.mainChannelToken();

        initPager();
        initCombo();

        postInitSetup();
    }

    public void postInitSetup() {
        deepLinkNavigate();

        Helper.initNavigationViewDrawer(this, toolbar);
        Helper.askStoragePermission(this);
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
        viewPager = (ViewPager) findViewById(R.id.scene_select_pager);
        viewPager.setAdapter(new SceneSelectFragmentPagerAdapter(getSupportFragmentManager(),
                SceneSelectActivity.this, channelToken));
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
        TabLayout tabLayout = (TabLayout) findViewById(R.id.scene_select_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }


    private void deepLinkNavigate() {
        Intent intent = getIntent();
        String sceneToken;
        String characterToken;

        if ((sceneToken = intent.getStringExtra("sceneTokenEditorDeepLink")) != null) {
            Intent newIntent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("channelToken", channelToken);
            newIntent.putExtra("characterToken", "");
            newIntent.putExtra("sceneToken", sceneToken);
            BardLogger.trace("[sceneDeepLink] " + sceneToken);
            startActivityForResult(newIntent, BARD_EDITOR_REQUEST_CODE);
        } else if ((characterToken = intent.getStringExtra("packTokenEditorDeepLink")) != null) {
            Intent newIntent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("channelToken", channelToken);
            newIntent.putExtra("characterToken", characterToken);
            newIntent.putExtra("sceneToken", "");
            BardLogger.trace("[packDeepLink] " + characterToken);
            startActivityForResult(newIntent, BARD_EDITOR_REQUEST_CODE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_search:
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra("channelToken", channelToken);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        BardLogger.log("Scene Select onResume");

        Helper.initNavigationViewDrawer(this, toolbar);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // go to home
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == BARD_EDITOR_REQUEST_CODE) {
            boolean shouldBackToChannel = data.getBooleanExtra("backToChannel", false);
            if (shouldBackToChannel) {
                // navigate to feed tab
                viewPager.setCurrentItem(viewPager.getAdapter().getCount());

            }
        } else if (resultCode == RESULT_OK && requestCode == LOGIN_REQUEST_CODE) {
        }
    }




    @Override
    public void onItemLongClick(Scene scene) {
        addComboItem(scene);
    }

    @Override
    public void onItemClick(Scene scene) {
        if (sceneComboList.isEmpty()) {
            Intent intent = new Intent(this, BardEditorActivity.class);
            intent.putExtra("channelToken", channelToken);
            intent.putExtra("characterToken", "");
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

    private void onWordListDownloadSuccess() {

    }

    private void onWordListDownloadFailure() {

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

}
