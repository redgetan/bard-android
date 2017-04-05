package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.ClipboardManager;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.*;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.ShareActionProvider;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.SmartFragmentStatePagerAdapter;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.ui.widget.CustomDialog;
import com.roplabs.bard.ui.widget.InputViewPager;
import com.roplabs.bard.ui.widget.SavePackDialog;
import com.roplabs.bard.ui.widget.WordsAutoCompleteTextView;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.*;
import java.lang.Process;
import java.lang.reflect.Field;
import java.util.*;

import static com.roplabs.bard.ClientApp.getContext;
import static com.roplabs.bard.util.Helper.*;

public class BardEditorActivity extends BaseActivity implements
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        Helper.KeyboardVisibilityListener, PopupMenu.OnMenuItemClickListener, SavePackDialog.OnPackDialogEvent {

    public static final String EXTRA_MESSAGE = "com.roplabs.bard.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.bard.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.bard.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.bard.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.bard.WORD_LIST";
    private static final int MAX_WORD_TAG_COUNT = 25;

    private boolean isPackSaved = false;

    private Context mContext;
    private RelativeLayout inputContainer;
    private ImageView findNextBtn;
    private ImageView findPrevBtn;
    private InputViewPager vpPager;
    private FrameLayout vpPagerContainer;
    private TextView debugView;
    private WordsAutoCompleteTextView editText;
    private String packageDir;
    private String applicationDir;
    private String ffmpegPath;
    public ProgressBar progressBar;
    private RecyclerView recyclerView;
    private SmartFragmentStatePagerAdapter adapterViewPager;
    private LinearLayout editTextContainer;
    private ImageView currentImageView;
    private ImageView lastImageView;
    private ProgressDialog progressDialog;
    private View lastClickedWordTagView;
    private LinearLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    private Handler wordTagAssignHandler;

    private Runnable notifyInvalidWordsRunnable;
    private Handler notifyInvalidWordsHandler;

    private boolean shouldSkipWordTagPlayback = false;
    private boolean isEditTextInitialized = false;
    private boolean isWordNavigatorInitialized = false;
    private NavigationView navigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;
    private CustomDialog loginDialog;
    private PopupMenu editorMenu;

    private Trie<String, String> wordTrie;
    private List<String> lastMergedWordTagList;
    private boolean skipOnTextChangeCallback;
    private TextView repoTitle;

    private String lastPlayedWordTag = "";
    private String channelToken;
    private String characterToken;
    private String sceneToken;
    private String sceneTokens;
    private Character character;
    private Scene scene;
    private Repo repo;
    private List<String> availableWordList;
    private String[] uniqueWordList;
    Set<String> invalidWords;
    private Button playMessageBtn;
    private Button addWordBtn;
    private LinearLayout videoResultContent;
    private WordListAdapter.ViewHolder lastViewHolder;
    private LinearLayout editorRootLayout;
    private Button saveRepoBtn;
    private ImageView sceneSelectBtn;
    private ImageView modeChangeBtn;
    private Runnable scrollToThumbnailRunnable;
    private Handler scrollToThumbnailHandler;
    private TextView word_tag_status;
    private TextView display_word_error;
    private FrameLayout previewContainer;
    private Runnable delayedWordPreviewPlayback;
    private Handler wordTagPlayHandler;
    private Map<String, String> wordListByScene;

    private WordTagSelector wordTagSelector;
    private TextureView previewTagView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;
    private Surface previewSurface;
    private View previewOverlay;
    private Runnable fetchWordTagSegmentUrl;
    private SavePackDialog savePackDialog;

    private int originalVideoHeight = -1;
    private Process process;
    private boolean isMergeInterrupted = false;

    ShareActionProvider mShareActionProvider;
    private String editTextbeforeChange = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bard_editor);
        mContext = this;

        debugView = (TextView) findViewById(R.id.display_debug);
        editorRootLayout = (LinearLayout) findViewById(R.id.editor_root_layout);

        packageDir = getExternalFilesDir(null).getAbsolutePath();

        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + Helper.ffmpegBinaryName();

        wordTagPlayHandler = new Handler();
        inputContainer = (RelativeLayout) findViewById(R.id.input_container);
        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        vpPagerContainer = (FrameLayout) findViewById(R.id.vp_pager_container);
        invalidWords = new HashSet<String>();
        editTextContainer = (LinearLayout) findViewById(R.id.bard_text_entry);
        playMessageBtn = (Button) findViewById(R.id.play_message_btn);
        videoResultContent = (LinearLayout) findViewById(R.id.video_result_content);

        recyclerView = (RecyclerView) findViewById(R.id.word_list_dictionary);
        recyclerView.setLayoutManager(new WordsLayoutManager(getContext()));
        recyclerView.setItemAnimator(null); // prevent blinking animation when notifyItemChanged on adapter is called
        recyclerView.setVisibility(View.GONE);
        playMessageBtn.setEnabled(true);

        initWordTagViewListeners();

//        addWordBtn = (Button) findViewById(R.id.add_word_btn);
//        addWordBtn.setEnabled(false);

        editText = (WordsAutoCompleteTextView) findViewById(R.id.edit_message);
        editText.setEnableAutocomplete(false);
        editText.setRecyclerView(recyclerView);
        editText.setEnabled(false);
        editText.setPrivateImeOptions("com.google.android.inputmethod.latin.noMicrophoneKey");

//        sceneSelectBtn = (ImageView) findViewById(R.id.scene_select_btn);
        modeChangeBtn = (ImageView) findViewById(R.id.mode_change_btn);
        modeChangeBtn.setAlpha(75);

        findNextBtn = (ImageView) findViewById(R.id.btn_find_next);
        findPrevBtn = (ImageView) findViewById(R.id.btn_find_prev);
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);

        lastMergedWordTagList = new ArrayList<String>();

        Intent intent = getIntent();
        characterToken = intent.getStringExtra("characterToken");
        channelToken = intent.getStringExtra("channelToken");
        sceneToken = intent.getStringExtra("sceneToken");
        sceneTokens = intent.getStringExtra("sceneTokens");
        if (sceneTokens == null) sceneTokens = "";
        character  = Character.forToken(characterToken);
        scene      = Scene.forToken(sceneToken);
        wordTagAssignHandler = new Handler();
        notifyInvalidWordsHandler = new Handler();
        scrollToThumbnailHandler = new Handler();

        previewContainer = (FrameLayout) findViewById(R.id.preview_container);
        display_word_error = (TextView) findViewById(R.id.display_word_error);
        word_tag_status = (TextView) findViewById(R.id.word_tag_status);
        previewTagView = (TextureView) findViewById(R.id.preview_tag_view);
        previewOverlay = findViewById(R.id.preview_video_overlay);
        wordTagPlayHandler = new Handler();
        word_tag_status.setText("");
        wordListByScene = new HashMap<String, String>();
        savePackDialog = new SavePackDialog(this);

        initVideoPlayer();


        initEmptyState();
        hideKeyboard();
        initVideoStorage();
        initChatText();
        updatePlayMessageBtnState();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        previewSurface = new Surface(surface);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setSurface(previewSurface);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {


    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        BardLogger.trace("mediaplayer onPrepared");
        isVideoReady = true;
        progressBar.setVisibility(View.GONE);

        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }


    private void initEmptyState() {
        emptyStateContainer = (LinearLayout) findViewById(R.id.empty_state_main_container);
        emptyStateTitle = (TextView) findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) findViewById(R.id.empty_state_description);

        emptyStateContainer.setVisibility(View.GONE);
    }

    public void restoreOriginalVideoHeight() {
        if (originalVideoHeight > -1) {
            ViewGroup.LayoutParams params = vpPagerContainer.getLayoutParams();
            params.height = originalVideoHeight;
            vpPagerContainer.setLayoutParams(params);
            adjustVideoAspectRatio();
        }
    }

    public void changeInputMode(View view) {
        if (editText.isFilteredAlphabetically()) {
            // change to sequential
            editText.setEnableAutocomplete(false);
            modeChangeBtn.setImageResource(R.drawable.ic_mode_edit_black_18dp);
            hideKeyboard();
            restoreOriginalVideoHeight();
        } else {
            // change to alphabetical filter mode
            editText.setEnableAutocomplete(true);
            modeChangeBtn.setImageResource(R.drawable.ic_keyboard_hide_black_18dp);
            showKeyboard();
            editText.requestFocus();
        }
    }

    private void initControls() {
        if (!sceneToken.isEmpty()) {
            // scene editor
            editText.setVisibility(View.GONE);
            editTextContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            // character editor

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("characterToken", characterToken);
        savedInstanceState.putString("sceneToken", characterToken);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        characterToken = savedInstanceState.getString("characterToken");
        sceneToken = savedInstanceState.getString("sceneToken");

    }

    public void onMoreBtnClick(View view) {
        editorMenu = new PopupMenu(this, view);
        editorMenu.setOnMenuItemClickListener(this);
        if (character != null) {
            editorMenu.inflate(R.menu.menu_pack_editor_more);
            setFavoritePackItemState();
        } else if (!sceneTokens.isEmpty()) {
            editorMenu.inflate(R.menu.menu_multi_scene_editor_more);
            setSavePackItemState();
        } else {
            editorMenu.inflate(R.menu.menu_scene_editor_more);
            setFavoriteSceneItemState();
        }
        editorMenu.show();
    }

    private void setSavePackItemState() {
        if (isPackSaved) {
            MenuItem item = editorMenu.getMenu().findItem(R.id.save_as_pack_item);
            item.setTitle("Pack Saved");
            item.setEnabled(false);
        }
    }

    private void setFavoriteSceneItemState() {
        MenuItem item = editorMenu.getMenu().findItem(R.id.favorite_scene_item);
        Favorite favorite = Favorite.forSceneTokenAndUsername(sceneToken, Setting.getUsername(this));
        if (favorite != null) {
            item.setTitle("Remove from My Videos");
        } else {
            item.setTitle("Add to My Videos");
        }
    }

    private void setFavoritePackItemState() {
        MenuItem item = editorMenu.getMenu().findItem(R.id.favorite_pack_item);
        String username = Setting.getUsername(this);
        if (character.getOwner().equals(username)) {
            item.setTitle("You own this pack");
            item.setEnabled(false);
            return;
        }

        UserPack userPack = UserPack.forPackTokenAndUsername(characterToken, username);
        if (userPack != null) {
            item.setTitle("Remove from packs");
        } else {
            item.setTitle("Add to packs");
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Context self = this;
        String url;
        Intent intent;
        switch (item.getItemId()) {
            case R.id.share_editor_item:
                intent = new Intent(this, ShareEditorActivity.class);
                intent.putExtra("sceneToken", sceneToken);
                startActivityForResult(intent, SHARE_SCENE_REQUEST_CODE);
                return true;
            case R.id.copy_editor_link_item:
                url = Configuration.bardAPIBaseURL() + "/scenes/" + sceneToken + "/editor";
                copyEditorLinkToClipboard(url);
                return true;
            case R.id.view_editor_source_item:
                String youtubeUrl = "https://www.youtube.com/watch?v=" + sceneToken;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)));
                return true;
            case R.id.favorite_scene_item:
                toggleSceneFavorite();
                return true;
            case R.id.save_as_pack_item:
                saveAsPackItem();
                return true;
            case R.id.favorite_pack_item:
                togglePackFavorite();

                return true;
            case R.id.share_pack_item:
                intent = new Intent(this, ShareEditorActivity.class);
                intent.putExtra("characterToken", characterToken);
                startActivityForResult(intent, SHARE_PACK_REQUEST_CODE);
                return true;
            case R.id.copy_pack_link_item:
                url = Configuration.bardAPIBaseURL() + "/packs/" + characterToken ;
                copyEditorLinkToClipboard(url);
                return true;
            default:
                return false;
        }
    }

    private void saveAsPackItem() {
        savePackDialog.show();
    }

    private void copyEditorLinkToClipboard(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(url, url);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ClientApp.getContext(), url + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    public void toggleSceneFavorite() {
        Favorite favorite = Favorite.forSceneTokenAndUsername(sceneToken, Setting.getUsername(this));
        if (favorite == null) {
            // create
            doFavoriteScene();
        } else {
            // delete
            doUnfavoriteScene(favorite);
        }
    }

    public void togglePackFavorite() {
        UserPack favorite = UserPack.forPackTokenAndUsername(characterToken, Setting.getUsername(this));
        if (favorite == null) {
            // create
            doFavoritePack();
        } else {
            // delete
            doUnfavoritePack(favorite);
        }
    }

    private void doFavoritePack() {
        // anonymous user, save pack locally only
        if (!Setting.isLogined(this)) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            UserPack.create(realm, characterToken, Setting.getUsername(ClientApp.getContext()));
            realm.commitTransaction();
            setFavoritePackItemState();

            return;
        }

        editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().favoritePack(characterToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                UserPack.create(realm, characterToken, Setting.getUsername(ClientApp.getContext()));
                realm.commitTransaction();

                setFavoritePackItemState();

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    private void doUnfavoritePack(final UserPack userPack) {
        // anonymous user, save pack locally only
        if (!Setting.isLogined(this)) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            userPack.deleteFromRealm();
            realm.commitTransaction();
            setFavoritePackItemState();

            return;
        }

        editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().unfavoritePack(characterToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                userPack.deleteFromRealm();
                realm.commitTransaction();

                setFavoritePackItemState();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                editorMenu.getMenu().findItem(R.id.favorite_pack_item).setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    private void doFavoriteScene() {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to bookmark a video");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().favoriteScene(sceneToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                Favorite.create(realm, sceneToken, Setting.getUsername(ClientApp.getContext()));
                realm.commitTransaction();

                setFavoriteSceneItemState();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    private void doUnfavoriteScene(final Favorite favorite) {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to bookmark a video");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(false);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().unfavoriteScene(sceneToken);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(true);

                if (response.code() != 200) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                favorite.deleteFromRealm();
                realm.commitTransaction();

                setFavoriteSceneItemState();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                editorMenu.getMenu().findItem(R.id.favorite_scene_item).setEnabled(true);

                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }
        });

    }

    @Override
    public void onSavePackConfirm(final String packName) {
        savePackDialog.dismiss();

        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to create pack");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
        } else {
            HashMap<String, String> packMap = new HashMap<String, String>();
            packMap.put("name", packName);
            packMap.put("scene_tokens", sceneTokens);

            progressBar.setVisibility(View.VISIBLE);

            Call<Character> call = BardClient.getAuthenticatedBardService().createPack(packMap);
            call.enqueue(new Callback<Character>() {
                @Override
                public void onResponse(Call<Character> call, Response<Character> response) {
                    progressBar.setVisibility(View.GONE);

                    if (response.code() != 200) {
                        return;
                    }

                    Character remotePack = response.body();
                    Character.create(remotePack);

                    Realm realm = Realm.getDefaultInstance();

                    realm.beginTransaction();
                    UserPack userPack = UserPack.create(realm, remotePack.getToken(), Setting.getUsername(ClientApp.getContext()));
                    realm.commitTransaction();
                    isPackSaved = true;

                    MenuItem item = editorMenu.getMenu().findItem(R.id.save_as_pack_item);
                }

                @Override
                public void onFailure(Call<Character> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);

                }
            });

        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    // when everything is built (i.e. creating the wordTrie)
    public interface OnWordListSetupListener {
        void onWordListPrepared();
    }

    private void setCharacterOrSceneTitle() {
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        if (!sceneToken.isEmpty()) {
            title.setText(scene.getName());
        } else {
//            title.setText(character.getName());
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    private void showKeyboard() {
        if (editText.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
        }
    }


    private void reloadWordTagViewData(List<String> wordTagStringList) {
        editText.setOriginalWordTagStringList(wordTagStringList);

        WordListAdapter adapter = new WordListAdapter(this, wordTagStringList);
        adapter.setIsWordTagged(true);
        adapter.setOnItemClickListener(new WordListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, WordTag wordTag) {
                // if its a different view, set previous one to true first to avoid getting stucked at disabled state
                if (lastClickedWordTagView != null && lastClickedWordTagView != itemView) {
                    lastClickedWordTagView.setEnabled(true);
                }
                lastClickedWordTagView = itemView;
                lastClickedWordTagView.setEnabled(false);

                if (!wordTag.isFilled()) {
                    // this will happen if we are on filtered mode, where wordTags in adapter is not tagged (to avoid duplicate results in recyclerview)
                    WordTag targetWordTag = getWordTagSelector().findNextWord(wordTag.word);

                    // if filtered wordTag is chosen, we want to show user original list
                    editText.displayOriginalWordList();
                    onWordTagClick(targetWordTag);
                } else {
                    onWordTagClick(wordTag);
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void initWordTagViewListeners() {
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });
    }

    private void initVideoStorage() {
        // where merged videos would be stored
        File moviesDirFile = new File(Storage.getSharedMoviesDir());

        if (!moviesDirFile.exists()) {
            moviesDirFile.mkdirs();
        }
    }

    public void initAnalytics() {
        Bundle params = new Bundle();
        params.putString("sceneToken", sceneToken);
        params.putString("scene", scene.getName());
        Analytics.track(this, "compose", params);
    }

    private void initChatText() {
        clearChatCursor();

        if (!sceneTokens.isEmpty()) {
            initMultiSceneWordList();
        } else if (!characterToken.isEmpty()) {
            if (character != null) {
                initCharacterWordList();
            } else {
                createCharacterWithWordList();
            }
        } else if (!sceneToken.isEmpty()) {
            if (scene != null) {
                initSceneWordList();
            } else {
                createSceneWithWordList();
            }
        }
    }

    private void clearChatCursor() {
        if (editText == null || Build.VERSION.SDK_INT < 12) {
            return;
        }
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.setInt(editText, 0);
        } catch (Exception e) {
            BardLogger.log(e.getMessage());
            e.printStackTrace();
        }
    }

    private void onWordListAvailable(List<String> wordTagStringList) {
        BardLogger.log("[MARIO] wordList available. wordTrie count: " + wordTrie.size());
        reloadWordTagViewData(wordTagStringList);

        initMultiAutoComplete();

        editText.setEnabled(true);
        recyclerView.setVisibility(View.VISIBLE);

        Helper.setKeyboardVisibilityListener(this, editorRootLayout);
    }

    private void createSceneWithWordList() {
        progressBar.setVisibility(View.VISIBLE);
        debugView.setText("Downloading");

        Call<Scene> call = BardClient.getAuthenticatedBardService().getScene(sceneToken);
        call.enqueue(new Callback<Scene>() {
            @Override
            public void onResponse(Call<Scene> call, Response<Scene> response) {
                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                Scene remoteScene = response.body();

                if (remoteScene == null) {
                    displayEmptyWordListError();
                    return;
                }

                String wordList = remoteScene.getWordList();

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                scene = Scene.create(realm, remoteScene.getToken(),"",remoteScene.getName(),remoteScene.getThumbnailUrl());
                scene.setWordList(wordList);
                realm.commitTransaction();

                if (wordList.isEmpty()) {
                    displayEmptyWordListError();
                } else {
                    asyncWordListSetup(wordList);
                }

                initAnalytics();
            }

            @Override
            public void onFailure(Call<Scene> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                debugView.setText("");
                displayEmptyWordListError();
            }
        });
    }

    private void initSceneWordList() {
        if (scene.getWordList().isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            debugView.setText("Downloading");

            Call<Scene> call = BardClient.getAuthenticatedBardService().getSceneWordList(sceneToken);
            call.enqueue(new Callback<Scene>() {
                @Override
                public void onResponse(Call<Scene> call, Response<Scene> response) {
                    progressBar.setVisibility(View.GONE);
                    debugView.setText("");

                    Scene remoteScene = response.body();

                    if (remoteScene == null) {
                        displayEmptyWordListError();
                        return;
                    }

                    String wordList = remoteScene.getWordList();

                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    scene.setWordList(wordList);
                    realm.commitTransaction();

                    if (wordList.isEmpty()) {
                        displayEmptyWordListError();
                    } else {
                        asyncWordListSetup(wordList);
                    }

                    initAnalytics();
                }

                @Override
                public void onFailure(Call<Scene> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    debugView.setText("");
                    displayEmptyWordListError();
                }
            });
        } else {
            asyncWordListSetup(scene.getWordList());
        }
    }

    private void displayEmptyWordListError() {
        progressBar.setVisibility(View.GONE);
        debugView.setText("");

        emptyStateTitle.setText("No Words Found");
        emptyStateDescription.setText("Failed to load word list from server. Try again later.");
        emptyStateContainer.setVisibility(View.VISIBLE);
//        Toast toast = Toast.makeText(getApplicationContext(), "Failed to load word list", Toast.LENGTH_LONG);
//        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
//        toast.show();
    }

    private void displayEmptySearchMessage() {
        emptyStateTitle.setVisibility(View.GONE);
        emptyStateDescription.setText("No results found. Try another word");
        emptyStateContainer.setVisibility(View.VISIBLE);
    }

    private void hideEmptySearchMessage() {
        emptyStateTitle.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void asyncWordListSetup(String wordListString) {
        progressBar.setVisibility(View.VISIBLE);
        debugView.setText("Initializing");

        (new AsyncTask<String, Integer, Void>() {
            @Override
            protected Void doInBackground(String... args) {
                String targetWordListString = args[0];
                addWordListToDictionary(targetWordListString);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                onWordListAvailable(availableWordList);
            }

        }).execute(wordListString);
    }

    private void asyncWordListSetup(String wordListString, final OnWordListSetupListener listener) {
        (new AsyncTask<String, Integer, Void>() {
            @Override
            protected Void doInBackground(String... args) {
                String targetWordListString = args[0];
                addWordListToDictionary(targetWordListString);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                progressBar.setVisibility(View.GONE);
                debugView.setText("");

                onWordListAvailable(availableWordList);
                listener.onWordListPrepared();
            }

        }).execute(wordListString);
    }

    private void createCharacterWithWordList() {
        progressBar.setVisibility(View.VISIBLE);
        debugView.setText("Downloading");

        Call<Character> call = BardClient.getAuthenticatedBardService().getCharacter(characterToken);
        call.enqueue(new Callback<Character>() {
            @Override
            public void onResponse(Call<Character> call, Response<Character> response) {
                Character remoteCharacter = response.body();

                if (remoteCharacter == null) {
                    displayEmptyWordListError();
                    return;
                }

                wordListByScene = remoteCharacter.getWordListByScene();

                List<String> combinedWordList = new ArrayList<String>();
                for (Map.Entry<String, String> wordListEntry : wordListByScene.entrySet()) {
                    String givenWordList = wordListEntry.getValue();
                    combinedWordList.add(givenWordList);
                }

                asyncWordListSetup(TextUtils.join(",",combinedWordList), new OnWordListSetupListener() {
                    @Override
                    public void onWordListPrepared() {
                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        character.setIsBundleDownloaded(true);
                        realm.commitTransaction();
                    }
                });

            }

            @Override
            public void onFailure(Call<Character> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                debugView.setText("");
                Toast.makeText(getApplicationContext(), "Failed to download word list", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initMultiSceneWordList() {
        wordListByScene = new HashMap<String, String>();
        String[] sceneTokenArray = sceneTokens.split(",");
        Scene targetScene;
        for (String targetSceneToken : sceneTokenArray) {
            targetScene = Scene.forToken(targetSceneToken);
            wordListByScene.put(targetSceneToken, targetScene.getWordList());
        }

        List<String> combinedWordList = new ArrayList<String>();
        for (Map.Entry<String, String> wordListEntry : wordListByScene.entrySet()) {
            String givenWordList = wordListEntry.getValue();
            combinedWordList.add(givenWordList);
        }

        asyncWordListSetup(TextUtils.join(",",combinedWordList));
    }


    private void initCharacterWordList() {

        if (character.getIsBundleDownloaded()) {
            Character character = Character.forToken(characterToken);

            this.wordListByScene = character.getWordListByScene();

            List<String> combinedWordList = new ArrayList<String>();
            for (Map.Entry<String, String> wordListEntry : wordListByScene.entrySet()) {
                String givenWordList = wordListEntry.getValue();
                combinedWordList.add(givenWordList);
            }

            asyncWordListSetup(TextUtils.join(",",combinedWordList));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            debugView.setText("Downloading");

            Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().getCharacterWordList(characterToken);
            call.enqueue(new Callback<HashMap<String, String>>() {
                @Override
                public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                    wordListByScene = response.body();

                    List<String> combinedWordList = new ArrayList<String>();
                    for (Map.Entry<String, String> wordListEntry : wordListByScene.entrySet()) {
                        String givenWordList = wordListEntry.getValue();
                        combinedWordList.add(givenWordList);
                    }

                    asyncWordListSetup(TextUtils.join(",",combinedWordList), new OnWordListSetupListener() {
                        @Override
                        public void onWordListPrepared() {
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            character.setWordListByScene(wordListByScene);
                            character.setIsBundleDownloaded(true);
                            realm.commitTransaction();
                        }
                    });

                }

                @Override
                public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    debugView.setText("");
                    Toast.makeText(getApplicationContext(), "Failed to download word list", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    private void addWordListToDictionary(String wordList) {
        List<String> givenWordList = new ArrayList<String>(Arrays.asList(wordList.split(",")));
        if (availableWordList != null) {

        } else {
            availableWordList = givenWordList;
            uniqueWordList = buildUniqueWordList();
            wordTrie = buildWordTrie();
        }

        initWordTagSelector(availableWordList);
    }

    // for a pack, a wordTagString can belong to one of the many sceneTokens it has
    // determine which one it belongs to
    public String getSceneTokenFromWordTagString(String wordTagString) {
        for (Map.Entry<String, String> wordListEntry : wordListByScene.entrySet()) {
            String givenWordList = wordListEntry.getValue();
            if (givenWordList.contains(wordTagString)) return wordListEntry.getKey();
        }

        return ""; // if none found, return empty string
    }

    private void addSceneWordListToDictionary(String wordList) {
        List<String> givenWordList = new ArrayList<String>(Arrays.asList(wordList.split(",")));

        this.getWordTagSelector().setSceneWordTagMap(givenWordList);
    }

    private String[] buildUniqueWordList() {
        String word = "";
        Set<String> wordSet = new HashSet<String>();

        for (String wordTagString : availableWordList) {
            word = wordTagString.split(":")[0];
            wordSet.add(word);
        }

        String[] list = new String[wordSet.size()];
        return wordSet.toArray(list);
    }


    private Trie<String, String> buildWordTrie() {
        Trie<String, String> trie = new PatriciaTrie<String>();
        for (String word : uniqueWordList ) {
            trie.put(word, null);
        }

        return trie;
    }

    public void initWordTagSelector(List<String> wordTagStringList) {
        wordTagSelector = new WordTagSelector(availableWordList);

        if (isWordNavigatorInitialized) {
            return;
        } else {
            isWordNavigatorInitialized = true;
        }

        findNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get wordTagString based from editText via tokenizer
                // set it to selector
                WordTag targetWordTag = getWordTagSelector().findNextWord();
                if (targetWordTag != null) {
                    BardLogger.trace("[findNext] " + targetWordTag.toString());
                    focusOnWordTag(targetWordTag);
                    skipOnTextChangeCallback = true;
                    editText.replaceLastText(targetWordTag.toString());
                    editText.format();
                    skipOnTextChangeCallback = false;
                    setWordTagWithDelay(targetWordTag, 500);
                }
            }
        });

        findPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WordTag targetWordTag = getWordTagSelector().findPrevWord();
                if (targetWordTag != null) {
                    BardLogger.trace("[findPrev] " + targetWordTag.toString());
                    focusOnWordTag(targetWordTag);
                    skipOnTextChangeCallback = true;
                    editText.replaceLastText(targetWordTag.toString());
                    editText.format();
                    skipOnTextChangeCallback = false;
                    setWordTagWithDelay(targetWordTag, 500);
                }
            }
        });
    }

    // only call this when dictionary already initialized (i.e. wordTrie has been built)
    private void initMultiAutoComplete() {

//        TrieAdapter<String> adapter =
//                new TrieAdapter<String>(this, android.R.layout.simple_list_item_1, availableWordList, wordTrie);
        editText.setAutoCompleteWords(wordTrie);

        if (isEditTextInitialized) {
            return;
        } else {
            isEditTextInitialized = true;
        }

        editText.setFilters(new InputFilter[] { handleSpaceKey() });

        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setScroller(new Scroller(this));
        editText.setMaxLines(1);
        editText.setVerticalScrollBarEnabled(true);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTokenizer(new SpaceTokenizer());
        editText.setOnFilterCompleteListener(new WordsAutoCompleteTextView.OnFilterCompleteListener() {
            @Override
            public void onFilterComplete(List<String> results) {
                if (results.isEmpty()) {
                    displayEmptySearchMessage();
                } else {
                    hideEmptySearchMessage();
                }

                // focus on first result
                if (!editText.getCurrentTokenWord().isEmpty() && results.size() > 0) {
                    WordTag wordTag = getWordTagSelector().findDefaultWord(results.get(0));
                    focusOnWordTag(wordTag);
                }

            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editTextbeforeChange = editText.getText().toString();

                if (!skipOnTextChangeCallback) {

                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isTextReduced = editText.getText().length() < editTextbeforeChange.length();

                if (!skipOnTextChangeCallback) {
                    handleUnavailableWords(s);

                    if (isTextReduced) {
                        ensureWordTagCleanDelete(start, count);
                    }

                    formatTagsIfNeeded();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!skipOnTextChangeCallback) {
                    editText.findPrefixMatches();
                }

                if (editText.getText().toString().isEmpty()) {
                    clearPreview();
                }
            }
        });

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                BardLogger.log("clicked editText, wordTag is: " + editText.getClickedWordTag());
                String wordTagString = editText.getClickedWordTag();
                WordTag wordTag = getWordTagSelector().getWordTagFromWordTagString(wordTagString);
                if (wordTag != null) {
                    focusOnWordTag(wordTag);
                    setWordTag(wordTag);
                }
            }
        });

    }

    // TEMP HACK to fix problem of:
    // before: "we are"
    // after: "weare" (about to delete we)
    // result: "weare" (we becomes merged with are)
    private void ensureWordTagCleanDelete(int start, int count) {
        if (editText.length() > 0 && editText.isImmediatelyAfterImageSpan()) {
            // backspace pressed
            // delete tagged word before it
            skipOnTextChangeCallback = true;
            int tokenEnd = start;

            // start would sometimes be in middle of text (i.e. swiftkey), make sure we're deleting starting at end of word
            if (start > 0 && editText.getText().charAt(start - 1) == ':') {
                tokenEnd = editText.findTokenEnd(start);
            }

            int amountToDelete = tokenEnd - editText.findTokenStart(tokenEnd);
            editText.getText().delete(tokenEnd - amountToDelete, tokenEnd);
            skipOnTextChangeCallback = false;
        }
    }

    private void formatTagsIfNeeded() {
        String lastCharacter = editText.getLastChar();
        boolean isLeaderPressed = lastCharacter.equals(" ");

        // make sure to add 'tag' around word that was just added
        if (isLeaderPressed) {
            skipOnTextChangeCallback = true;
            editText.format();
            skipOnTextChangeCallback = false;
        }
    }


    private boolean isKeyAllowed(String targetChar) {
       return targetChar.matches("[\\w\\s]"); // only allow alphanumeric and space
    }

    // if invalid, dont proceed
    // if valid, autocomplete partial word with current selection

    public InputFilter handleSpaceKey() {
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.length() == 1 && !isKeyAllowed(source.toString())) {
                    return "";
                } else if (source.length() == 1 && source.charAt(0) == ' ' && editText.containsInvalidWord()) {
                    return "";
                } else if (source.length() == 1 && source.charAt(0) == ' ' && !editText.toString().trim().isEmpty() && !editText.getCurrentTokenWord().isEmpty()) {
                    // user press space in valid state
                    // user press spaced
                    List<String> filteredResults = editText.getFilteredResults();
                    if (!filteredResults.isEmpty()) {
                        // if valid wordTag completion is performed, we want original word list to show
                        editText.displayOriginalWordList();

                        String firstMatch = filteredResults.get(0);
                        if (!firstMatch.contains(":")) {
                            // make sure we only process untagged filtered result
                            WordTag wordTag = getWordTagSelector().findDefaultWord(firstMatch);
                            setWordTag(wordTag);

                            // autocompletion at work
                            return wordTag.toString().substring(editText.getCurrentTokenWord().length()) + " ";
                        } else {
                            return null;
                        }
                    }
                    return "";
                } else if (source.length() == 1 && editText.isBeforeImageSpan()){
                    // prevent user from adding words in between imagespans (easier from implementation perspective)
                    // TODO: remove this restriction, allow user to add words in between
                    return "";
                } else if (source.length() == 1 && (editText.length() > 0) && (editText.isImmediatelyAfterImageSpan())) {
                    // end of sentence but right after a word
                    // add a space before
                    return " " + source;
                } else {
                    return null;
                }

            }
        };

        return filter;
    }

    // http://stackoverflow.com/a/10864568
    public TextView createContactTextView(String text){
        //creating textview dynamically
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20);
        tv.setBackgroundResource(R.drawable.bordered_rectangle_rounded_corners);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_black_18dp, 0);
        return tv;
    }

    public static Object convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(viewBmp);

    }

    private void updatePlayMessageBtnState() {
        if (getFilledWordTagCount() > 0) {
//            playMessageBtn.setEnabled(true);
//            playMessageBtn.setVisibility(View.VISIBLE);
        } else {
//            playMessageBtn.setEnabled(false);
//            playMessageBtn.setVisibility(View.GONE);
        }
    }

    private void handleUnavailableWords(CharSequence s) {
        if (notifyInvalidWordsRunnable != null) {
            notifyInvalidWordsHandler.removeCallbacks(notifyInvalidWordsRunnable);
        }

        notifyInvalidWordsRunnable = new Runnable(){
            @Override
            public void run(){
                notifyUserOnUnavailableWord();
                notifyInvalidWordsRunnable = null;
            }
        };

        notifyInvalidWordsHandler.postDelayed(notifyInvalidWordsRunnable, 500);

    }

    private void onWordTagClick(WordTag wordTag) {
        if (!editText.isEnabled()) return; // if editText is disabled, dont allow adding to it

        skipOnTextChangeCallback = true;

        String toInsert = "";
        int cursorPosition = editText.getSelectionEnd();
        if (cursorPosition == -1) {
            cursorPosition = 0;
        }
        if (cursorPosition == editText.length()) {
            // end of sentence (replace instead of insert)
            toInsert =  null;
        } else if (cursorPosition == 0 && editText.getText().charAt(1) != ' ') {
            // insert beginning of sentence
            toInsert = wordTag.toString() + " ";
        } else if (editText.getText().charAt(cursorPosition - 1) != ' ') {
            // insert middle of sentence
            // there's character before cursor
            toInsert =  " " + wordTag.toString();
        } else if (editText.getText().charAt(cursorPosition + 1) != ' ') {
            // insert middle of sentence
            // there's character after cursor
            toInsert =  wordTag.toString() + " ";
        }

        if (toInsert == null) {
            editText.replaceText(wordTag.toString() + " ");
        } else {
            editText.getText().insert(cursorPosition, toInsert);
        }

        editText.format();
        skipOnTextChangeCallback = false;

        focusOnWordTag(wordTag);

        setWordTag(wordTag);
        updatePlayMessageBtnState();

        BardLogger.log("onWordTagClick: " + editText.getText().toString());
    }


    public void addWord(View view) {
        editText.getText().insert(editText.getSelectionStart(), " ");
    }

    public void onWordTagChanged(final WordTag wordTag, int delayInMilliSeconds) {
        if (wordTag == null) return;

        if (!wordTag.isFilled()) return;
        BardLogger.trace("onWordTagChanged: " + wordTag.toString());

        drawPagination();
        drawWordTagNavigatorState();

        if (delayedWordPreviewPlayback != null) {
            wordTagPlayHandler.removeCallbacks(delayedWordPreviewPlayback);
        }

        delayedWordPreviewPlayback = new Runnable(){
            @Override
            public void run(){
                playRemoteVideoAndDisplayThubmnail(wordTag.toString());
                delayedWordPreviewPlayback = null;
            }
        };

        if (shouldSkipWordTagPlayback) {
            // dont play preview, but allow next one
            shouldSkipWordTagPlayback = false;
        } else {
            wordTagPlayHandler.postDelayed(delayedWordPreviewPlayback, delayInMilliSeconds);
        }
    }

    private void focusOnWordTag(WordTag wordTag) {
        if (wordTag == null) return;

        BardLogger.log("focusing...." + wordTag.word);
        int position;

        if (editText.isFilteredAlphabetically() && !editText.isUnfiltered()) {
            position = editText.getFilteredResults().indexOf(wordTag.word);
        } else {
            position = wordTag.position;
        }

        ((WordListAdapter) recyclerView.getAdapter()).selectViewByPosition(position);
        if (recyclerView.findViewHolderForAdapterPosition(position) == null) {
            // if view is outside of recycylerview bounds, scroll to it
            recyclerView.scrollToPosition(position);
        }
    }

    private void drawWordTagNavigatorState() {
        findNextBtn.setVisibility(View.VISIBLE);
        findPrevBtn.setVisibility(View.VISIBLE);

        if (getWordTagSelector().getCurrentWordTagCount() > 1) {
            findPrevBtn.setColorFilter(ContextCompat.getColor(this, R.color.md_light_blue_500));
            findNextBtn.setColorFilter(ContextCompat.getColor(this, R.color.md_light_blue_500));
        } else {
            findPrevBtn.setColorFilter(ContextCompat.getColor(this, R.color.md_grey_500));
            findNextBtn.setColorFilter(ContextCompat.getColor(this, R.color.md_grey_500));
        }
    }

    private int getFilledWordTagCount() {
        int count = 0;

        String[] tokens = editText.getText().toString().split("\\s+");
        for (String wordTagString : tokens) {
            if (wordTagString.contains(":")) {
                count++;
            }
        }

        return count;
    }

    private void clearPreview() {
        getWordTagSelector().clearWordTag();
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);
        word_tag_status.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CustomDialog.LOGIN_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(getContext(), "Login successful", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.SIGNUP_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(getContext(), "Account successfully created", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == SHARE_REPO_REQUEST_CODE) {
            setResult(RESULT_OK);
            finish();
        } else if (resultCode == RESULT_OK && requestCode == EDITOR_PREVIEW_REQUEST_CODE) {
            boolean shouldBackToChannel = data.getBooleanExtra("backToChannel", false);
            if (shouldBackToChannel) {
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }

    private void loadSceneThumbnail(Scene scene) {
        if (scene == null) {
            Bitmap allSceneBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_add_black_24dp);
            this.sceneSelectBtn.setImageBitmap(allSceneBitmap);
            this.sceneSelectBtn.setAlpha(0.3f);
        } else {
            Glide.with(this)
                    .load(scene.getThumbnailUrl())
                    .placeholder(R.drawable.thumbnail_placeholder)
                    .crossFade()
                    .into(this.sceneSelectBtn);
            this.sceneSelectBtn.setAlpha(1.0f);
        }
    }

    private boolean notifyUserOnUnavailableWord() {
        skipOnTextChangeCallback = true;
        editText.format();
        skipOnTextChangeCallback = false;

        return false;
    }


    @Override
    protected void onStop() {
        super.onStop();
        hideKeyboard();
    }

    private void setControlState(boolean enabled) {
        playMessageBtn.setEnabled(enabled);
        recyclerView.setEnabled(enabled);
        findNextBtn.setEnabled(enabled);
        findPrevBtn.setEnabled(enabled);
        editText.setEnabled(enabled);
    }

    private void disableControls() {
        setControlState(false);
    }

    private void enableControls() {
        setControlState(true);
    }

    // use ffmpeg binary to concat videos hosted in cloudfront (run in background thread)
    public void joinSegments(final List<String> wordTagList) {
        Analytics.timeEvent(this, "generateBardVideo");

        final BardEditorActivity self = this;

        disableControls();
        restoreOriginalVideoHeight();
        progressBar.setVisibility(View.VISIBLE);

        List<Segment> segments = Segment.buildFromWordTagList(wordTagList, sceneToken);
        final String outputFilePath = Storage.getMergedOutputFilePath();
        // delete old one before merging (since we rely on checking presence to see if merge is success or not)
        if ((new File(outputFilePath)).exists()) {
            new File(outputFilePath).delete();
        }
        final String wordList = getWordListFromSegments(segments);

        // get only segments that exist
        List<Segment> validSegments = new ArrayList<Segment>();
        for (Segment segment : segments) {
            if (new File(segment.getFilePath()).exists()) {
                validSegments.add(segment);
            }
        }

        String[] cmd = buildJoinSegmentsCmd(validSegments, outputFilePath);
        final long startTime = System.currentTimeMillis();
        BardLogger.trace(TextUtils.join(",",cmd));

        (new AsyncTask<String[], Integer, String>() {
            @Override
            protected String doInBackground(String[]... cmds) {
                return Helper.runCmd(cmds[0], new ProcessListener() {
                    @Override
                    public void onProcessAvailable(Process process) {
                        self.process = process;
                    }
                });
            }

            @Override
            protected void onPostExecute(String result) {
                BardLogger.trace("joinSegments onPostExecute");
                BardLogger.trace(result);
                enableControls();
                // check if file was created
                if (isMergeInterrupted) {
                    return;
                } else if ((new File(outputFilePath)).exists()) {
                    final long endTime = System.currentTimeMillis();
                    BardLogger.log(String.valueOf(endTime - startTime) + " seconds" );
                    lastMergedWordTagList = wordTagList;
                    onJoinSegmentsSuccess();
                } else {
                    // report error
                    CrashReporter.logException(new Throwable(result));
                }
            }

        }).execute(cmd);

    }

    private void onJoinSegmentsSuccess() {
        // remember result (for sharing)
        playMessageBtn.setEnabled(true);
        word_tag_status.setText("");
        progressBar.setVisibility(View.GONE);

        hideKeyboard();
        trackGenerateBardVideo();
        goToEditorResultsPreview();
    }

    private String getWordListFromSegments(List<Segment> segments) {
        List<String> list = new ArrayList<String>();
        for (Segment segment: segments) {
            list.add(segment.getWord());
        }
        return TextUtils.join(" ", list);
    }

    public String[] buildJoinSegmentsCmd(List<Segment> segments, String outputFilePath) {
        List<String> cmd = new ArrayList<String>();

        cmd.add(ffmpegPath);
        cmd.add("-y");

        for (Segment segment : segments) {
            if (new File(segment.getFilePath()).exists()) {
                cmd.add("-i");
                cmd.add(segment.getFilePath());
            }
        }

        if (segments.size() > 1) {
            cmd.add("-filter_complex");
            cmd.add(buildConcatFilterGraph(segments));
            cmd.add("-map");
            cmd.add("[v]");
            cmd.add("-map");
            cmd.add("[a]");
        }

        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("ultrafast");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add("-strict");
        cmd.add("-2");

        cmd.add(outputFilePath);

        return cmd.toArray(new String[0]);
    }

    private String buildConcatFilterGraph(List<Segment> segments) {
        String concatFilterGraph = "";
        int index;

        for (Segment segment : segments) {
            index = segments.indexOf(segment);
            concatFilterGraph += " [" + index + ":v] " + "[" + index + ":a] ";
        }

        concatFilterGraph += " concat=n=" + segments.size() + ":v=1:a=1 [v] [a] ";

        return concatFilterGraph;
    }

    public void closeEditor(View view) {
        if (process != null) {
            isMergeInterrupted = true;
            process.destroy();
        }
        finish();
    }

    private void tagLastWordIfPartial() {
        String lastWord = editText.getCurrentTokenWord();
        boolean lastWordNotTagged = !lastWord.trim().isEmpty() && !lastWord.contains(":");
        List<String> filteredResults = editText.getFilteredResults();
        if (lastWordNotTagged && !filteredResults.isEmpty()) {
            String firstMatch = filteredResults.get(0);
            // make sure last word matches the one in filtered results
            if (!firstMatch.contains(lastWord)) return;

            WordTag wordTag = getWordTagSelector().findNextWord(firstMatch);
            setWordTag(wordTag);
            editText.replaceText(wordTag.toString() + " ");
        }
    }

    public void generateBardVideo(View view) throws IOException {
        tagLastWordIfPartial();

        List<String> wordTagList = new ArrayList<String>(Arrays.asList(editText.getText().toString().split("\\s+")));
        wordTagList.removeAll(Collections.singletonList("")); // dont allow empty strings
        if (wordTagList.isEmpty()) return; // if not words to merge exit

        BardLogger.trace("[generateBardVideo] editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());

        // replay last merge if nothing changed
        if (lastMergedWordTagList.toString().equals(wordTagList.toString())) {
            goToEditorResultsPreview();
            return;
        }

        if (editText.containsInvalidWord()) return;
        if (!isAllWordsTagged(wordTagList)) return;

        if (wordTagList.size() > MAX_WORD_TAG_COUNT) {
            Toast.makeText(getContext(),"Cannot exceed " + MAX_WORD_TAG_COUNT + " words", Toast.LENGTH_LONG).show();
            return;
        }

        lastMergedWordTagList = wordTagList;
        goToEditorResultsPreview();

//        joinSegments(wordTagList);
    }

    public List<String> getWordTagList() {
        String[] tokens = editText.getText().toString().split("\\s+");
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].isEmpty()) {
                result.add(tokens[i]);
            }
        }

        return result;
    }

    private boolean isAllWordsTagged(List<String> wordTagList) {
        for (String wordTagString : wordTagList) {
            if (!wordTagString.contains(":")) return false;
        }

        return true;
    }


    private void trackGenerateBardVideo() {

        Bundle params = new Bundle();

        params.putString("wordTags", TextUtils.join(",", lastMergedWordTagList));
        if (scene != null) {
            params.putString("sceneToken", sceneToken);
            params.putString("scene", scene.getName());
        } else if (character != null) {
            params.putString("packToken", characterToken);
            params.putString("pack", character.getName());
        }
        params.putString("length", String.valueOf(lastMergedWordTagList.size()));

        Analytics.track(this, "generateBardVideo", params);
    }

    private void playRemoteVideoAndDisplayThubmnail(String wordTagString) {

        if (!Helper.isConnectedToInternet()) {
            debugView.setText(R.string.no_network_connection);
            return;
        }

        String filePath = Storage.getCachedVideoFilePath(wordTagString);
        if (new File(filePath).exists()) {
            playLocalVideo(filePath);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            playMessageBtn.setEnabled(false);

            // if were editing a character/pack, sceneToken is blank, we need to find which scene a wordTagString belongs to
            String targetSceneToken;
            if (sceneToken.isEmpty()) {
                targetSceneToken = getSceneTokenFromWordTagString(wordTagString);
            } else {
                targetSceneToken = sceneToken;
            }
            String remoteVideoUrl = Segment.sourceUrlFromWordTagString(wordTagString, targetSceneToken);
            Storage.cacheVideo(wordTagString, remoteVideoUrl, new Storage.OnCacheVideoListener() {
                @Override
                public void onCacheVideoSuccess(String filePath) {
                    playMessageBtn.setEnabled(true);
                    BardLogger.trace("video cached at " + filePath);
                    playLocalVideo(filePath);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCacheVideoFailure() {
                    playMessageBtn.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),"Failed to download word preview", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void playLocalVideo(String filePath) {
        progressBar.setVisibility(View.GONE);
        debugView.setText("");

        if (previewOverlay.isShown()) previewOverlay.setVisibility(View.GONE);
        playVideo(filePath);

        if (lastClickedWordTagView != null) {
            lastClickedWordTagView.setEnabled(true);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void setVideoError(String error) {
        debugView.setText(error);
    }

    @Override
    public void onKeyboardVisibilityChanged(boolean keyboardVisible, int keyboardHeight) {
        int keyboardWordTagDiff = keyboardHeight - recyclerView.getHeight() - 30;
        boolean isKeyboardShown = keyboardWordTagDiff > 0;
        if (isKeyboardShown) {

            // adjust video size
            ViewGroup.LayoutParams params = vpPagerContainer.getLayoutParams();
            if (originalVideoHeight == -1) {
               originalVideoHeight = params.height;
            }
            params.height = (int) (originalVideoHeight / 1.25);
            vpPagerContainer.setLayoutParams(params);
            adjustVideoAspectRatio();

            // change to alphabetical filter mode
            editText.setEnableAutocomplete(true);
            modeChangeBtn.setImageResource(R.drawable.ic_keyboard_hide_black_18dp);

        }
    }

    private void adjustVideoAspectRatio() {

        ViewTreeObserver vto = vpPagerContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                fixVideoAspectRatio();

                ViewTreeObserver obs = vpPagerContainer.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });

    }

    public void fixVideoAspectRatio() {
        // keep aspect ratio to 16/9
        Matrix txform = new Matrix();
        previewTagView.getTransform(txform);
        int viewHeight = previewTagView.getHeight();
        int viewWidth = previewTagView.getWidth();
        int newHeight = viewHeight;
        int newWidth = (int) (1.9 * viewHeight);
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        previewTagView.setTransform(txform);

    }


    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = previewTagView.getWidth();
        int viewHeight = previewTagView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        BardLogger.log("video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        previewTagView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        previewTagView.setTransform(txform);
    }


    public WordTagSelector getWordTagSelector() {
        return wordTagSelector;
    }

    public void setWordTag(WordTag wordTag) {
        BardLogger.trace("setWordTag: " + wordTag.toString());
        wordTagSelector.setWordTag(wordTag);
        onWordTagChanged(wordTag, 0);
    }

    public void setWordTagWithDelay(WordTag wordTag, int delayInMilliSeconds) {
        BardLogger.trace("setWordTag with delay: " + wordTag.toString());
        wordTagSelector.setWordTag(wordTag);
        onWordTagChanged(wordTag, delayInMilliSeconds);
    }

    private void drawPagination() {
        StringBuilder builder = new StringBuilder();

//        builder.append(wordTagSelector.getCurrentWord());
//        builder.append(" (");
        builder.append(wordTagSelector.getCurrentWordTagIndex() + 1);
        builder.append(" of ");
        builder.append(wordTagSelector.getCurrentWordTagCount());
//        builder.append(" )");

        word_tag_status.setText(builder.toString());
    }

    private void initVideoPlayer() {
        // video
        previewTagView.setOpaque(false);
        previewTagView.setSurfaceTextureListener(this);

        previewTagView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                if (wordTagSelector == null) return;
                WordTag targetWordTag = wordTagSelector.findNextWord();
                if (targetWordTag != null) onWordTagChanged(targetWordTag, 500);
            }

            @Override
            public void onSwipeRight() {
                if (wordTagSelector == null) return;
                WordTag targetWordTag = wordTagSelector.findPrevWord();
                if (targetWordTag != null) onWordTagChanged(targetWordTag, 500);
            }

            @Override
            public void onTouchUp() {
                if (!isVideoReady) return;

                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });


        ViewTreeObserver vto = previewTagView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {

                ViewTreeObserver obs = previewTagView.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });

    }


    public void playVideo(String sourceUrl) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(sourceUrl);
            mediaPlayer.setSurface(previewSurface);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
    }

    public void resetVideo() {
        previewOverlay.setVisibility(View.VISIBLE);
    }

    private void goToEditorResultsPreview() {
        Intent intent = new Intent(this, EditorPreviewActivity.class);

        intent.putExtra("wordTags", TextUtils.join(",",lastMergedWordTagList));
        intent.putExtra("sceneToken", sceneToken);
        intent.putExtra("characterToken", characterToken);
        intent.putExtra("channelToken", channelToken);
        if (scene != null) {
            intent.putExtra("sceneName", scene.getName());
        }
        startActivityForResult(intent, EDITOR_PREVIEW_REQUEST_CODE);
    }


}
