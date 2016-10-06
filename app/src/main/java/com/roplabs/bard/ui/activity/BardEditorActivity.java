package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.*;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.instabug.library.Instabug;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.InputPagerAdapter;
import com.roplabs.bard.adapters.ShareListAdapter;
import com.roplabs.bard.adapters.SmartFragmentStatePagerAdapter;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.events.*;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.ui.fragment.VideoResultFragment;
import com.roplabs.bard.ui.fragment.WordListFragment;
import com.roplabs.bard.ui.widget.CustomDialog;
import com.roplabs.bard.ui.widget.InputViewPager;
import com.roplabs.bard.ui.widget.SquareImageView;
import com.roplabs.bard.ui.widget.WordsAutoCompleteTextView;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import io.realm.RealmResults;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class BardEditorActivity extends BaseActivity implements
        WordListFragment.OnReadyListener,
        WordListFragment.OnWordTagChanged,
        WordListFragment.OnPreviewPlayerPreparedListener, Helper.KeyboardVisibilityListener, AdapterView.OnItemClickListener {

    public static final String EXTRA_MESSAGE = "com.roplabs.bard.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.bard.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.bard.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.bard.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.bard.WORD_LIST";
    private final int SCENE_SELECT_REQUEST_CODE = 20;


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
    private int currentTokenIndex;
    private ProgressDialog progressDialog;

    private Runnable attemptWordTagAssignRunnable;
    private Handler wordTagAssignHandler;

    private Runnable notifyInvalidWordsRunnable;
    private Handler notifyInvalidWordsHandler;

    private boolean isEditTextInitialized = false;
    private boolean isWordNavigatorInitialized = false;
    private NavigationView navigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;
    private CustomDialog loginDialog;

    private Trie<String, String> wordTrie;
    private LinkedList<WordTag> wordTagList;
    private boolean skipOnTextChangeCallback;
    private TextView repoTitle;

    private Boolean isWordTagListContainerBlocked;
    private String characterToken;
    private String sceneToken;
    private Character character;
    private Scene scene;
    private Repo repo;
    private List<String> availableWordList;
    private String[] uniqueWordList;
    Set<String> invalidWords;
    private Button playMessageBtn;
    private ImageView toggleWordListBtn;
    private LinearLayout previewTimeline;
    private LinearLayout previewTimelineContainer;
    private LinearLayout videoResultContent;
    private HorizontalScrollView previewTimelineScrollView;
    private WordListAdapter.ViewHolder lastViewHolder;
    private LinearLayout editorRootLayout;
    private GridView shareListView;
    private Button saveRepoBtn;
    private ImageView sceneSelectBtn;

    ShareActionProvider mShareActionProvider;

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

        inputContainer = (RelativeLayout) findViewById(R.id.input_container);
        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        vpPagerContainer = (FrameLayout) findViewById(R.id.vp_pager_container);
        invalidWords = new HashSet<String>();
        wordTagList = new LinkedList<WordTag>();
        editTextContainer = (LinearLayout) findViewById(R.id.bard_text_entry);
        playMessageBtn = (Button) findViewById(R.id.play_message_btn);
        toggleWordListBtn = (ImageView) findViewById(R.id.toggleWordListBtn);
        previewTimeline = (LinearLayout) findViewById(R.id.preview_timeline);
        previewTimelineContainer = (LinearLayout) findViewById(R.id.preview_timeline_container);
        videoResultContent = (LinearLayout) findViewById(R.id.video_result_content);
        previewTimelineScrollView = (HorizontalScrollView) findViewById(R.id.preview_timeline_scrollview);
        recyclerView = (RecyclerView) findViewById(R.id.word_list_dictionary);
        recyclerView.setLayoutManager(new WordsLayoutManager(ClientApp.getContext()));
        initWordTagViewListeners();

        editText = (WordsAutoCompleteTextView) findViewById(R.id.edit_message);
        editText.setEnableAutocomplete(false);
        editText.setRecyclerView(recyclerView);

        shareListView = (GridView) findViewById(R.id.social_share_list);
        saveRepoBtn = (Button) findViewById(R.id.save_repo_btn);
        sceneSelectBtn = (ImageView) findViewById(R.id.scene_select_btn);

        findNextBtn = (ImageView) findViewById(R.id.btn_find_next);
        findPrevBtn = (ImageView) findViewById(R.id.btn_find_prev);
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);
        previewTimelineScrollView.setHorizontalScrollBarEnabled(false);
        isWordTagListContainerBlocked = false;

        Intent intent = getIntent();
        characterToken = intent.getStringExtra("characterToken");
        sceneToken = intent.getStringExtra("sceneToken");
        character  = Character.forToken(characterToken);
        scene      = Scene.forToken(sceneToken);
        wordTagAssignHandler = new Handler();
        notifyInvalidWordsHandler = new Handler();

        Helper.setKeyboardVisibilityListener(this, editorRootLayout);

        initPreviewTimeline();
        initVideoStorage();
        initAnalytics();
        initViewPager();
        initMultiAutoComplete();
        updatePlayMessageBtnState();
        initShare();
    }

    private void initShare() {
        // { name: "messenger", icon: "" }


//        PackageManager packageManager = getPackageManager();
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, "");
//        shareIntent.setType("video/mp4");
//
//        List<ResolveInfo> items = packageManager.queryIntentActivities(shareIntent, 0);

//        apps = sortAppSharing(apps);

        String apps[] = new String[] { "messenger", "whatsapp", "kik", "telegram", "twitter", "tumblr"} ;

        ShareListAdapter shareListAdapter = new ShareListAdapter(this, apps);
        shareListView.setAdapter(shareListAdapter);
        shareListView.setOnItemClickListener(this);
    }

//    private List<ResolveInfo> sortAppSharing(List<ResolveInfo> apps) {
//        List<ResolveInfo> sortedApps = new ArrayList<ResolveInfo>();
//
//        for (ResolveInfo app : apps) {
//            if (app.activityInfo.packageName.equals("com.facebook.orca")) {
//                sortedApps.add(app);
//            }
//        }
//
//        return sortedApps;
//    }

    private void initControls() {
        if (!sceneToken.isEmpty()) {
            // scene editor
            editText.setVisibility(View.GONE);
            toggleWordListBtn.setVisibility(View.GONE);
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

    private void setCharacterOrSceneTitle() {
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        if (!sceneToken.isEmpty()) {
            title.setText(scene.getName());
        } else {
            title.setText(character.getName());
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
        WordListAdapter adapter = new WordListAdapter(this, wordTagStringList);
        adapter.setIsWordTagged(true);
        adapter.setOnItemClickListener(new WordListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, WordTag wordTag) {
                onWordTagClick(wordTag);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void initWordTagViewListeners() {
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return isWordTagListContainerBlocked;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });
    }

    private void initPreviewTimeline() {
        // add blank preview slots
        ImageView imageView;

        for (int i = 0; i < 20; i++) {
            imageView = createPreviewImageView(null);
            previewTimeline.addView(imageView,i);
        }
    }

    private void initVideoStorage() {
        File moviesDirFile = new File(getSharedMoviesDir());

        if (!moviesDirFile.exists()) {
            moviesDirFile.mkdirs();
        }
    }

    private String getSharedMoviesDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                   .getAbsolutePath() + "/" +  getResources().getString(R.string.app_name) + "/";
    }

    public void initAnalytics() {
        Tracker mTracker = ((ClientApp) getApplication()).getDefaultTracker();
        mTracker.setScreenName("ChatActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void initViewPager() {
        vpPager = (InputViewPager) findViewById(R.id.vpPager);

        adapterViewPager = new InputPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);
        vpPager.setAllowedSwipeDirection(InputViewPager.SwipeDirection.none);

        // Attach the page change listener inside the activity
        vpPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Code goes here
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
                // Code goes here
            }
        });
    }


    private void initChatText() {
        clearChatCursor();
        initDictionary();
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

    private void initDictionary() {
        progressBar.setVisibility(View.VISIBLE);
        debugView.setText("Initializing Available Word List");

        if (!sceneToken.isEmpty()) {
            initSceneWordList();
        } else {
            initCharacterWordList();
        }
    }

    private void onWordListAvailable(List<String> wordTagStringList) {
        reloadWordTagViewData(wordTagStringList);
        progressBar.setVisibility(View.GONE);
        debugView.setText("");
        previewTimelineScrollView.setHorizontalScrollBarEnabled(true);
    }

    private void initSceneWordList() {
        if (scene.getWordList() == null) {
            Call<Scene> call = BardClient.getAuthenticatedBardService().getSceneWordList(characterToken, sceneToken);
            call.enqueue(new Callback<Scene>() {
                @Override
                public void onResponse(Call<Scene> call, Response<Scene> response) {
                    Scene remoteScene = response.body();
                    String wordList = remoteScene.getWordList();

                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    scene.setWordList(wordList);
                    realm.commitTransaction();

                    addSceneWordListToDictionary(wordList);
                    onWordListAvailable(scene.getWordListAsList());
                }

                @Override
                public void onFailure(Call<Scene> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    debugView.setText("");
                    Toast.makeText(getApplicationContext(), "Failed to load word list", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            addSceneWordListToDictionary(scene.getWordList());
            onWordListAvailable(scene.getWordListAsList());
        }
    }

    private void initCharacterWordList() {

        if (character.getIsBundleDownloaded()) {
            RealmResults<Scene> scenes = Scene.forCharacterToken(characterToken);

            final List<String> combinedWordList = new ArrayList<String>();
            for (Scene scene : scenes) {
                combinedWordList.add(scene.getWordList());
            }

            (new AsyncTask<String, Integer, Void>() {
                @Override
                protected Void doInBackground(String... wordListCombined) {
                    addWordListToDictionary(TextUtils.join(",", wordListCombined));
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    progressBar.setVisibility(View.GONE);
                    debugView.setText("");

                    onWordListAvailable(availableWordList);
                }

            }).execute(TextUtils.join(",",combinedWordList));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            debugView.setText("Initializing Available Word List");

            Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().getCharacterWordList(characterToken);
            call.enqueue(new Callback<HashMap<String, String>>() {
                @Override
                public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                    HashMap<String, String> wordListBySceneToken = response.body();
                    List<String> combinedWordList = new ArrayList<String>();
                    for (Map.Entry<String, String> wordListEntry : wordListBySceneToken.entrySet()) {
                        String givenSceneToken = wordListEntry.getKey();
                        String givenWordList = wordListEntry.getValue();

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        Scene scene = Scene.forToken(givenSceneToken);

                        if (scene == null) {
                            scene = Scene.create(realm, givenSceneToken, characterToken, "", "");
                            scene.setWordList(givenWordList);
                        }

                        realm.commitTransaction();
                        combinedWordList.add(givenWordList);
                    }

                    (new AsyncTask<String, Integer, Void>() {
                        @Override
                        protected Void doInBackground(String... wordListCombined) {
                            addWordListToDictionary(TextUtils.join(",", wordListCombined));
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            // mark character bundle as downloaded (full wordlist available)
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            character.setIsBundleDownloaded(true);
                            realm.commitTransaction();

                            progressBar.setVisibility(View.GONE);
                            debugView.setText("");

                            onWordListAvailable(availableWordList);
                        }

                    }).execute(TextUtils.join(",",combinedWordList));

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

    private void addSceneWordListToDictionary(String wordList) {
        List<String> givenWordList = new ArrayList<String>(Arrays.asList(wordList.split(",")));

        this.getWordTagSelector().setSceneWordTagMap(givenWordList);
    }

    private void loadSceneDictionary() {
        // this is so that there's no annoying horizontal scrollbar showing up for a few seconds (will be re-enabled later)
        previewTimelineScrollView.setHorizontalScrollBarEnabled(false);

        if (scene == null) {
            this.getWordTagSelector().setSceneWordTagMap(new HashMap<String, List<WordTag>>());
            onWordListAvailable(availableWordList);
        } else {
            initSceneWordList();
        }
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
        getWordListFragment().initWordTagSelector(availableWordList);
        initWordNavigatorListeners();
    }

    private void initWordNavigatorListeners() {
        if (isWordNavigatorInitialized) {
            return;
        } else {
            isWordNavigatorInitialized = true;
        }

        findNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WordTag targetWordTag = getWordTagSelector().findNextWord();
                if (targetWordTag != null) {
                    BardLogger.trace("[findNext] " + targetWordTag.toString());
                    int tokenIndex = editText.getTokenIndex();
                    wordTagList.set(tokenIndex, targetWordTag);
                    getWordListFragment().setWordTag(targetWordTag);
                }
            }
        });

        findPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WordTag targetWordTag = getWordTagSelector().findPrevWord();
                if (targetWordTag != null) {
                    BardLogger.trace("[findPrev] " + targetWordTag.toString());
                    int tokenIndex = editText.getTokenIndex();
                    wordTagList.set(tokenIndex, targetWordTag);
                    getWordListFragment().setWordTag(targetWordTag);
                }
            }
        });
    }

    private void initMultiAutoComplete() {

//        TrieAdapter<String> adapter =
//                new TrieAdapter<String>(this, android.R.layout.simple_list_item_1, availableWordList, wordTrie);
//        editText.setAutoCompleteWords(wordTrie);

        if (isEditTextInitialized) {
            return;
        } else {
            isEditTextInitialized = true;
        }

        editText.setScroller(new Scroller(this));
        editText.setMaxLines(1);
        editText.setVerticalScrollBarEnabled(true);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTokenizer(new SpaceTokenizer());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!skipOnTextChangeCallback) {
                    handleUnavailableWords(s, start);
                    updateWordTagList(s, start);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BardLogger.trace("[editText click] - select_start: " + editText.getSelectionStart() + " select_end: " + editText.getSelectionEnd() + " editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());
                if (editText.getTokenizer() != null) {
                    int tokenIndex = editText.getTokenIndex();
                    if (tokenIndex < wordTagList.size()) {
                        setCurrentImageView((ImageView) previewTimeline.getChildAt(tokenIndex));
                        currentTokenIndex = tokenIndex;
                        WordTag wordTag = wordTagList.get(tokenIndex);
                        if (wordTag != null && wordTag.isFilled()) {
                            getWordListFragment().setWordTag(wordTag);
                        }
                    }
                }
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (recyclerView.isShown()) {
                    recyclerView.setVisibility(View.GONE);
                }

                return false;
            }
        });
    }

    private void updatePlayMessageBtnState() {
        if (getTimelineEnabledImageViewCount() > 1) {
            playMessageBtn.setEnabled(true);
            playMessageBtn.setVisibility(View.VISIBLE);
        } else {
            playMessageBtn.setEnabled(false);
            playMessageBtn.setVisibility(View.GONE);
        }
    }

    private void handleUnavailableWords(CharSequence s, int start) {
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

        notifyInvalidWordsHandler.postDelayed(notifyInvalidWordsRunnable, 2000);

    }

    private void onWordTagClick(WordTag wordTag) {
        progressBar.setVisibility(View.VISIBLE);
        isWordTagListContainerBlocked = true;

        int beforeTokenCount = editText.getTokenCount();

        skipOnTextChangeCallback = true;
        editText.replaceSelectedText(" " + wordTag.word + " ");
        skipOnTextChangeCallback = false;

        int afterTokenCount = editText.getTokenCount();

        currentTokenIndex = editText.getTokenIndex();

        if (afterTokenCount > beforeTokenCount) {
            previewTimeline.addView(createPreviewImageView(null), currentTokenIndex);
            wordTagList.add(currentTokenIndex,wordTag);
        } else {
            wordTagList.set(currentTokenIndex,wordTag);
        }

        BardLogger.trace("[WordTag click] editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());

        setCurrentImageView((ImageView) previewTimeline.getChildAt(currentTokenIndex));

        getWordListFragment().setWordTag(wordTag);
    }

    @Override
    public void onWordTagChanged(WordTag wordTag) {
        drawWordTagNavigatorState();
        recyclerView.scrollToPosition(wordTag.position);
        playRemoteVideo(Segment.sourceUrlFromWordTagString(wordTag.toString()));
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

    private void onSuccessfulWordTagAssign(WordTag wordTag, int tokenIndex) {
        if (wordTag != null && !wordTagList.get(tokenIndex).isFilled()) {
            setImageViewAndAddIfNeeded(tokenIndex);

            // assign tag
            wordTagList.get(tokenIndex).tag = wordTag.tag;
            getWordListFragment().setWordTag(wordTag);
        }
    }

    private void onSuccessfulWordTagAdd(WordTag wordTag, int tokenIndex) {
        if (wordTag != null) {
            setImageViewAndAddIfNeeded(tokenIndex);
            wordTagList.add(tokenIndex,wordTag);
            getWordListFragment().setWordTag(wordTag);
        }
    }

    private void setImageViewAndAddIfNeeded(int tokenIndex) {
        // if number of enabled imageview in timeline is less than number of words in wordtaglist
        // insert at current character a new bitmap slot for onVideoThumbnail changed to fill,
        // else change current bitmap slot
        if (getTimelineEnabledImageViewCount() < wordTagList.size()) {
            ImageView emptyImageView = createPreviewImageView(null);
            previewTimeline.addView(emptyImageView, tokenIndex);
            setCurrentImageView(emptyImageView);
        } else {
            setCurrentImageView((ImageView) previewTimeline.getChildAt(tokenIndex));
        }
    }


    private void updateWordTagList(CharSequence s, int start) {
        clearAssignWOrdTagRunnable();

        String character = editText.getAddedChar(start);
        String nextCharacter = editText.getNextChar(s, start);
        boolean isLeaderPressed = character.equals(" ");
        boolean isBackspacePressed = character.equals("");
        String lastWord = editText.getLastWord();
        int tokenIndex = editText.getTokenIndex();

        if (isLeaderPressed && !nextCharacter.equals(" ") && !nextCharacter.equals("")) {
            lastWord = editText.getPrevWord(start);
            tokenIndex--;
        }
        currentTokenIndex = tokenIndex;
        WordTag wordTag = null;

        if (isLeaderPressed && (wordTagList.size() > tokenIndex)) {
            wordTag = getWordTagSelector().findRandomWord(lastWord);

            if (editText.getTokenCount() > wordTagList.size()) {
                if (wordTag != null) {
                    // will reach here if (before: iam, after: i am, char: " ")
                    // must insert new wordtag
                    onSuccessfulWordTagAdd(wordTag, tokenIndex);
                } else {
                    // will reach here if (before: iam, after: i am, char: " "), but word is not in dictionary
                    wordTagList.add(tokenIndex, new WordTag(lastWord));
                }

                // also update next neighboring wordtag
                String nextImmediateWord = editText.getText().toString().subSequence(start, editText.length()).toString().trim();
                wordTagList.get(tokenIndex + 1).word = nextImmediateWord;
                wordTagList.get(tokenIndex + 1).tag = "";
            } else {
                // get here if word was previous untagged, and we want to tag it
                onSuccessfulWordTagAssign(wordTag, tokenIndex);
            }
        } else {
            int tokenCount = editText.getTokenCount();
            if (tokenCount > wordTagList.size()) {
                // ADD wordTag (when token count increases)
                wordTag = new WordTag(lastWord);
                wordTagList.add(tokenIndex, wordTag);
                attemptAssignWordTagDelayed(lastWord, tokenIndex);
            } else if (tokenCount < wordTagList.size()) {
                // DELETE wordTag (when token count decreases)
                String nextWordInWordTagList = "";
                String nextImmediateWord = editText.getText().toString().subSequence(start, editText.length()).toString().trim();
                if (tokenIndex + 1 < wordTagList.size()) {
                    nextWordInWordTagList = wordTagList.get(tokenIndex + 1).word;
                } else {
                    nextWordInWordTagList = "";
                }
                if (nextImmediateWord.equals(nextWordInWordTagList)) {
                    // if the wordtag were deleting is still tagged, it means the thumbnail hasnt been deleted yet
                    // in the case, delete it
                    // ie. deleting ("i:2093jasdf")
                    //     deleting space between ["i:2093jasdf","was:xer93sd"]
                    if (wordTagList.get(tokenIndex).isFilled()) {
                        clearPreview();
                        removeThumbnailFromTimeline(tokenIndex);
                    }
                    wordTagList.remove(tokenIndex);
                } else {
                    if (wordTagList.get(tokenIndex).isFilled()) {
                        clearPreview();
                        removeThumbnailFromTimeline(tokenIndex + 1);
                    }
                    wordTagList.remove(tokenIndex + 1);
                }

            } else if (tokenCount == wordTagList.size() && tokenCount != 0) {
                // UPDATE wordTag (when word changed)
                String nextImmediateWord = editText.getText().toString().subSequence(start, editText.length()).toString().trim();
                wordTag = wordTagList.get(tokenIndex);
                String wordInWordTagList;

                if (wordTag != null) {
                    wordInWordTagList = wordTag.word;
                } else {
                    wordInWordTagList = "";
                }

                if (nextImmediateWord.equals(wordInWordTagList)) {
                    // dont change word tag (might be used again)
                } else {
                    if (wordTag != null && !wordTag.word.equals(lastWord)) {
                        // UPDATE the word at at current index
                        if (wordTag.isFilled()) {
                            // if the wordTag is previously filled, we need to reset wordTagSelector
                            // and also remove thumbnail from previewtimeline
                            clearPreview();
                            removeThumbnailFromTimeline(tokenIndex);
                        }
                        wordTag.tag = "";
                        wordTag.word = lastWord;

                        attemptAssignWordTagDelayed(lastWord, tokenIndex);
                    }
                }

            }

        }

        BardLogger.trace("[updateWordTag] editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());
    }

    private void clearPreview() {
        getWordTagSelector().clearWordTag();
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);
    }

    private void removeThumbnailFromTimeline(int tokenIndex) {
        ImageView imageView = (ImageView) previewTimeline.getChildAt(tokenIndex);
        if (imageView != null) {
            previewTimeline.removeView(imageView);
            previewTimeline.addView(createPreviewImageView(null));
            updatePlayMessageBtnState();
        }
    }

    private void attemptAssignWordTagDelayed(final String word, final int tokenIndex) {
        clearAssignWOrdTagRunnable();

        attemptWordTagAssignRunnable = new Runnable(){
            @Override
            public void run(){
                attemptAssignWordTag(word, tokenIndex);
                attemptWordTagAssignRunnable = null;
            }
        };

        wordTagAssignHandler.postDelayed(attemptWordTagAssignRunnable, 1500);
    }

    private void clearAssignWOrdTagRunnable() {
        if (attemptWordTagAssignRunnable != null) {
            wordTagAssignHandler.removeCallbacks(attemptWordTagAssignRunnable);
        }
    }

    private void attemptAssignWordTag(String word, int tokenIndex) {

        WordTag targetWordTag = getWordTagSelector().findRandomWord(word);
        if (targetWordTag != null) {
            onSuccessfulWordTagAssign(targetWordTag, tokenIndex);
        }
    }

    public void shareRepo(View view) {
        startActivity(Intent.createChooser(getRepoShareIntent(), "Share"));
    }

    public void toggleWordList(View view) {
        if (recyclerView.isShown()) {
            recyclerView.setVisibility(View.GONE);
            this.showKeyboard();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            this.hideKeyboard();
        }
    }

    public void startSceneSelect(View view) {
        Intent intent = new Intent(this, SceneSelectActivity.class);
        intent.putExtra("characterToken", character.getToken());
        intent.putExtra("previousSceneToken", sceneToken);
        startActivityForResult(intent, SCENE_SELECT_REQUEST_CODE);
//        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CustomDialog.LOGIN_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(ClientApp.getContext(), "Login successful", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.SIGNUP_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(ClientApp.getContext(), "Account successfully created", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == SCENE_SELECT_REQUEST_CODE) {
            sceneToken = data.getExtras().getString("sceneToken");
            scene = Scene.forToken(sceneToken);
            loadSceneDictionary();
            loadSceneThumbnail(scene);

            JSONObject properties = new JSONObject();
            try {
                properties.put("sceneToken", sceneToken);
            } catch (JSONException e) {
                e.printStackTrace();
                Instabug.reportException(e);
            }
            Analytics.track(this, "sceneSelect", properties);
        }
    }

    private void loadSceneThumbnail(Scene scene) {
        if (scene == null) {
            Bitmap allSceneBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_crop_original_black_24dp);
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

    private int getTimelineEnabledImageViewCount() {
        int childCount = previewTimeline.getChildCount();
        int enabledCount = 0;

        for (int i = 0; i < childCount; i++) {
            if (previewTimeline.getChildAt(i).isEnabled()) {
                enabledCount++;
            }
        }

        return enabledCount;
    }

    private void updateInvalidWords() {
        Editable text = editText.getText();
        List<String> words = Arrays.asList(text.toString().trim().split("\\s+"));
        invalidWords.retainAll(words);

        displayInvalidWords();
    }

    private void notifyUserOnUnavailableWord() {
        Editable text = editText.getText();
        String[] words = text.toString().toLowerCase().trim().split("\\s+");

        invalidWords.clear();

        for (String word : words) {
            if (!word.isEmpty() && !wordTrie.containsKey(word)) {
                invalidWords.add(word);
            }
        }

        displayInvalidWords();
    }


    private void displayInvalidWords() {
        if (invalidWords.size() > 0) {
            EventBus.getDefault().post(new InvalidWordEvent("Words not available: " + TextUtils.join(",",invalidWords)));
        } else {
            EventBus.getDefault().post(new InvalidWordEvent(""));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideKeyboard();
    }

    // use ffmpeg binary to concat videos hosted in cloudfront (run in background thread)
    public void joinSegments(List<Segment> segments) {
        final String outputFilePath = getJoinedOutputFilePath();
        final String wordList = getWordListFromSegments(segments);
        String[] cmd = buildJoinSegmentsCmd(segments, outputFilePath);
        final long startTime = System.currentTimeMillis();
        BardLogger.log(TextUtils.join(",",cmd));

        (new AsyncTask<String[], Integer, String>() {
            @Override
            protected String doInBackground(String[]... cmds) {
                return Helper.runCmd(cmds[0]);
            }

            @Override
            protected void onPostExecute(String result) {
                // check if file was created
                if ((new File(outputFilePath)).exists()) {
                    final long endTime = System.currentTimeMillis();
                    BardLogger.log(String.valueOf(endTime - startTime) + " seconds" );
                    onJoinSegmentsSuccess(outputFilePath);
                } else {
                    // report error
                    Instabug.reportException(new Throwable(result));
                }
            }

        }).execute(cmd);

    }

    private void onJoinSegmentsSuccess(String outputFilePath) {
        trackGenerateBardVideo();
        showVideoResultFragment();
        playLocalVideo(outputFilePath);

        playMessageBtn.setEnabled(true);
    }

//    private void setRepoTitle() {
//        repoTitle.setText(formatWordTagListTitle(wordTagList));
//    }

    private String formatWordTagListTitle(List<WordTag> wordTags) {
        List<String> phrase = new ArrayList<String>();

        for (WordTag wordTag : wordTags) {
            phrase.add(wordTag.word);
        }

        return TextUtils.join(" ",phrase);
    }


    private String getWordListFromSegments(List<Segment> segments) {
        List<String> list = new ArrayList<String>();
        for (Segment segment: segments) {
            list.add(segment.getWord());
        }
        return TextUtils.join(" ", list);
    }

    private String getRepositoryS3Key(String uuid) {
        return "repositories/" + Setting.getUsername(this) + "/" + uuid + ".mp4";
    }

    private void askUserToLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You must login in order to save the video to your profile")
                .setTitle("Login");

        builder.setPositiveButton("Login/Register", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setNegativeButton(R.string.instabug_str_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void saveRepo(View view) {
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this);
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();

//            askUserToLogin();
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivityForResult(intent, LOGIN_REQUEST_CODE);
            return;
        }

        saveRepoBtn.setEnabled(false);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Saving...");
        progressDialog.show();

        final String wordList = TextUtils.join(",", wordTagList);

        // upload to S3

        final String uuid = UUID.randomUUID().toString();
        AmazonS3 s3 = new AmazonS3Client(AmazonCognito.credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        TransferObserver observer = transferUtility.upload(
                Configuration.s3UserBucket(),
                getRepositoryS3Key(uuid),
                new File(getJoinedOutputFilePath())
        );

        observer.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                // do something
                if (state == TransferState.COMPLETED) {
                    saveRemoteRepo(uuid, wordList);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                //Display percentage transfered to user
            }

            @Override
            public void onError(int id, Exception ex) {
                // do something
                displayError("Unable to upload to server", ex);
            }

        });

    }

    private void saveRemoteRepo(String uuid, final String wordList) {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("uuid", uuid);
        body.put("word_list", wordList);
        body.put("character_token", this.characterToken);
        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postRepo(body);

        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (!response.isSuccess()) {
                    displayError("Unable to sync to remote server", new Throwable("Failed to save repo to bard server"));
                } else {
                    HashMap<String, String> result = response.body();
                    saveLocalRepo(result.get("token"), result.get("url"), wordList);
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                displayError("Unable to sync to remote server", t);
            }
        });
    }

    private void displayError(String message, Throwable t) {
        saveRepoBtn.setEnabled(true);
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        Instabug.reportException(t);
    }

    private void displayError(String message) {
        saveRepoBtn.setEnabled(true);
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        Instabug.reportException(new Throwable(message));
    }

    private void saveLocalRepo(String token, String url, String wordList) {
        String filePath = getSharedMoviesDir() + Helper.getTimestamp() + ".mp4";

        if (Helper.copyFile(getJoinedOutputFilePath(),filePath)) {
            this.repo = Repo.create(token, url, characterToken, sceneToken, filePath, wordList, Calendar.getInstance().getTime());

            saveRepoBtn.setText("Saved");
            progressDialog.dismiss();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setResult(RESULT_OK);
                    finish();
                }
            }, 500);

        } else {
            displayError("Unable to save to phone");
        }
    }


    public String[] buildJoinSegmentsCmd(List<Segment> segments, String outputFilePath) {
        List<String> cmd = new ArrayList<String>();

        cmd.add(ffmpegPath);
        cmd.add("-y");

        for (Segment segment : segments) {
            cmd.add("-i");
            cmd.add(segment.getFilePath());
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

    public String getJoinedOutputFilePath() {
        return getSharedMoviesDir() + "/last_merge.mp4";
    }

    public void closeEditor(View view) {
        finish();
    }

    public void generateBardVideo(View view) throws IOException {
        BardLogger.trace("[generateBardVideo] editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());
        playMessageBtn.setEnabled(false);

        if (Helper.isConnectedToInternet()) {
            if (addMissingWordTag()) {
                progressBar.setVisibility(View.VISIBLE);

                Analytics.timeEvent(this, "generateBardVideo");

                Runnable runnable = new Runnable() {
                    public void run() {
                        VideoDownloader.fetchSegments(Segment.buildFromWordTagList(wordTagList));
                    }
                };

                new Thread(runnable).start();
            } else {
                playMessageBtn.setEnabled(true);
                notifyUserOnUnavailableWord();

                if (invalidWords.isEmpty()) {
                    EventBus.getDefault().post(new InvalidWordEvent("Something's wrong. Please clear your sentence and restart from beginning"));
                }
            }
        } else {
            playMessageBtn.setEnabled(true);
            // display error
            debugView.setText(R.string.no_network_connection);
            return;
        }
    }

    public List<WordTag> getWordTagList() {
        return wordTagList;
    }

    private void trackGenerateBardVideo() {

        JSONObject properties = new JSONObject();

        try {
            properties.put("wordTags", wordTagList);
            properties.put("characterToken", characterToken);
            properties.put("sceneToken", sceneToken);
            properties.put("character", character.getName());
            if (scene != null) {
                properties.put("scene", scene.getName());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Instabug.reportException(e);
        }

        Analytics.track(this, "generateBardVideo", properties);
        Analytics.sendQueuedEvents(this);
    }

    // return false if wordtag missing and unable to find match. true otherwise
    public boolean addMissingWordTag() {
        for (WordTag wordTag : wordTagList) {
            if (wordTag.tag.isEmpty()) {
                WordTag targetWordTag = getWordTagSelector().findRandomWord(wordTag.word);
                if (targetWordTag == null) {
                    return false;
                } else {
                    wordTag.tag = targetWordTag.tag;
                }
            }

        }

        return true;
    }

    private WordTagSelector getWordTagSelector() {
        return  getWordListFragment().getWordTagSelector();
    }

    private WordListFragment getWordListFragment() {
        return (WordListFragment) adapterViewPager.getRegisteredFragment(0);
    }

    private VideoResultFragment getVideoResultFragment() {
        return (VideoResultFragment) adapterViewPager.getRegisteredFragment(1);
    }

    private void playRemoteVideo(String url) {
        if (!Helper.isConnectedToInternet()) {
            debugView.setText(R.string.no_network_connection);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (getWordListFragment() != null) {
            getWordListFragment().playPreview(url);
        }
    }

    public void playLocalVideo(String filePath) {
        progressBar.setVisibility(View.GONE);
        debugView.setText("");

        getVideoResultFragment().playLocalVideo(filePath);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    private void showVideoResultFragment() {
        hideKeyboard();

        editTextContainer.setVisibility(View.GONE);
        previewTimelineContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        playMessageBtn.setVisibility(View.GONE);
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);

        videoResultContent.setVisibility(View.VISIBLE);

        if (vpPager.getCurrentItem() != 1) {
            vpPager.setCurrentItem(1, true);
        }
    }

    public void showWordListFragment(View view) {
        videoResultContent.setVisibility(View.GONE);

        findNextBtn.setVisibility(View.VISIBLE);
        findPrevBtn.setVisibility(View.VISIBLE);
        editTextContainer.setVisibility(View.VISIBLE);
        previewTimelineContainer.setVisibility(View.VISIBLE);
        if (!recyclerView.isShown()) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        updatePlayMessageBtnState();

        if (vpPager.getCurrentItem() != 0) {
            vpPager.setCurrentItem(0, true);
        }

//        setCharacterOrSceneTitle();
    }

    @Subscribe
    public void onEvent(VideoDownloadEvent event) {
        if (event.error != null) {
            setVideoError(event.error);
            progressBar.setVisibility(View.GONE);
            Instabug.reportException(new Throwable(event.error));
        } else if (event.segments != null) {
            joinSegments(event.segments);
        }
    }

    private void setVideoError(String error) {
        debugView.setText(error);
    }

    public Intent getRepoShareIntent() {
        Uri videoUri;

        if (this.repo == null) {
            videoUri = Uri.fromFile(new File(getJoinedOutputFilePath()));
        } else {
            videoUri = Uri.fromFile(new File(this.repo.getFilePath()));
        }
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }

    @Override
    public void onWordListFragmentReady() {
        initChatText();
        if (getWordListFragment() != null) {
            getWordListFragment().setOnWordTagChangedListener(this);
            getWordListFragment().setOnPreviewPlayerPreparedListener(this);
        }
    }




    @Override
    public void onVideoThumbnailChanged(Bitmap bitmap) {
        if (currentImageView != null) {
            currentImageView.setImageBitmap(bitmap);
            currentImageView.setEnabled(true);
            updatePlayMessageBtnState();
        } else {
            currentImageView = createPreviewImageView(bitmap);
            previewTimeline.addView(currentImageView, currentTokenIndex);
        }

        currentImageView.setSelected(true);

        isWordTagListContainerBlocked = false;
    }

    public ImageView createPreviewImageView(Bitmap bitmap) {
        ImageView imageView = new SquareImageView(this);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setEnabled(false);
        }

        imageView.setBackgroundResource(R.drawable.selector_preview_image);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentImageView((ImageView) v);
                v.setSelected(true);

                int tokenIndex = previewTimeline.indexOfChild(v);

                BardLogger.trace("[imageView click] image: " + tokenIndex + " editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());

                HashMap<String,Integer> result = SpaceTokenizer.findStartStopOfNthToken(editText.getText(), tokenIndex);
                if (result.get("start") >= 0 && result.get("stop") >= 0) {
                    editText.setSelection(result.get("start"), result.get("stop"));
                }

                if (tokenIndex < wordTagList.size()) {
                    WordTag wordTag = wordTagList.get(tokenIndex);
                    if (wordTag.isFilled()) {
                        getWordListFragment().setWordTag(wordTag);
                    }
                }
            }
        });

        return imageView;
    }

    private void setCurrentImageView(ImageView imageView) {
        currentImageView = imageView;

        if (lastImageView != null && lastImageView != imageView) {
            lastImageView.setSelected(false);
        }

        lastImageView = currentImageView;
    }

    @Override
    public void onPreviewPlayerPrepared() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onKeyboardVisibilityChanged(boolean keyboardVisible, int keyboardHeight) {
        int keyboardWordTagDiff = keyboardHeight - recyclerView.getHeight();
        if (keyboardWordTagDiff > 0) {
            ViewGroup.LayoutParams params = vpPagerContainer.getLayoutParams();
            params.height = vpPagerContainer.getHeight() - keyboardWordTagDiff;
            vpPagerContainer.setLayoutParams(params);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String app = (String) view.getTag();

        if (app.equals("messenger")) {
            startMessengerShare();
        } else if (app.equals("whatsapp")) {
            startWhatsappShare();
        } else if (app.equals("kik")) {
            startKikShare();
        } else if (app.equals("telegram")) {
            startTelegramShare();
        } else if (app.equals("twitter")) {
            startTwitterShare();
        } else if (app.equals("tumblr")) {
            startTumblrShare();
        }

    }

    private void startMessengerShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.facebook.orca");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Facebook Messenger", Toast.LENGTH_LONG).show();
        }
    }

    private void startWhatsappShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Whatsapp", Toast.LENGTH_LONG).show();
        }
    }

    private void startTelegramShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("org.telegram.messenger");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Telegram", Toast.LENGTH_LONG).show();
        }
    }

    private void startTwitterShare() {
        Intent intent = getRepoShareIntent();
        intent.setClassName("com.twitter.android", "com.twitter.android.composer.ComposerActivity");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Twitter", Toast.LENGTH_LONG).show();
        }
    }

    private void startKikShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("kik.android");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Kik", Toast.LENGTH_LONG).show();
        }
    }

    private void startTumblrShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.tumblr");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Tumblr", Toast.LENGTH_LONG).show();
        }
    }
}
