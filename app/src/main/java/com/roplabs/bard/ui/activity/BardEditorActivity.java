package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.*;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.ShareActionProvider;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.crypto.ByteRangeCapturingInputStream;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.disklrucache.DiskLruCache;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
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
import io.fabric.sdk.android.services.common.Crash;
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
import java.util.concurrent.CountDownLatch;

public class BardEditorActivity extends BaseActivity implements
        WordListFragment.OnReadyListener,
        WordListFragment.OnWordTagChanged,
        WordListFragment.OnPreviewPlayerPreparedListener, Helper.KeyboardVisibilityListener {

    public static final String EXTRA_MESSAGE = "com.roplabs.bard.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.bard.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.bard.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.bard.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.bard.WORD_LIST";
    private final int SHARE_REQUEST_CODE = 20;

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
    private WordListFragment wordListFragment;
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

    private Trie<String, String> wordTrie;
    private List<String> lastMergedWordTagList;
    private boolean skipOnTextChangeCallback;
    private TextView repoTitle;

    private Boolean isWordTagListContainerBlocked;
    private String lastPlayedWordTag = "";
    private String characterToken;
    private String sceneToken;
    private Character character;
    private Scene scene;
    private Repo repo;
    private List<String> availableWordList;
    private String[] uniqueWordList;
    Set<String> invalidWords;
    private Button playMessageBtn;
    private Button addWordBtn;
    private LinearLayout previewTimelineContainer;
    private LinearLayout videoResultContent;
    private WordListAdapter.ViewHolder lastViewHolder;
    private LinearLayout editorRootLayout;
    private Button saveRepoBtn;
    private Button shareRepoBtn;
    private ImageView shareRepoIcon;
    private ImageView sceneSelectBtn;
    private ImageView modeChangeBtn;
    private Runnable scrollToThumbnailRunnable;
    private Handler scrollToThumbnailHandler;

    private int originalVideoHeight = -1;

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
        previewTimelineContainer = (LinearLayout) findViewById(R.id.preview_timeline_container);
        videoResultContent = (LinearLayout) findViewById(R.id.video_result_content);
        wordListFragment = new WordListFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, wordListFragment)
                .commit();

        recyclerView = (RecyclerView) findViewById(R.id.word_list_dictionary);
        recyclerView.setLayoutManager(new WordsLayoutManager(ClientApp.getContext()));
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
        editText.setPrivateImeOptions("nm");
        editText.setPrivateImeOptions("com.google.android.inputmethod.latin.noMicrophoneKey");

        shareRepoBtn = (Button) findViewById(R.id.share_repo_btn);
        shareRepoBtn.setVisibility(View.GONE);
        shareRepoIcon = (ImageView) findViewById(R.id.share_repo_icon);
        shareRepoIcon.setColorFilter(ContextCompat.getColor(this, R.color.md_green_400));
        shareRepoIcon.setVisibility(View.GONE);

//        sceneSelectBtn = (ImageView) findViewById(R.id.scene_select_btn);
        modeChangeBtn = (ImageView) findViewById(R.id.mode_change_btn);
        modeChangeBtn.setAlpha(75);

        findNextBtn = (ImageView) findViewById(R.id.btn_find_next);
        findPrevBtn = (ImageView) findViewById(R.id.btn_find_prev);
        findNextBtn.setVisibility(View.GONE);
        findPrevBtn.setVisibility(View.GONE);
        isWordTagListContainerBlocked = false;

        lastMergedWordTagList = new ArrayList<String>();

        Intent intent = getIntent();
        characterToken = "";
        sceneToken = intent.getStringExtra("sceneToken");

        scene      = Scene.forToken(sceneToken);
        wordTagAssignHandler = new Handler();
        notifyInvalidWordsHandler = new Handler();
        scrollToThumbnailHandler = new Handler();

        initEmptyState();
        hideKeyboard();
        initVideoStorage();
        initAnalytics();
        updatePlayMessageBtnState();
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
        Tracker mTracker = ((ClientApp) getApplication()).getDefaultTracker();
        mTracker.setScreenName("ChatActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        JSONObject properties = new JSONObject();
        try {
            properties.put("sceneToken", sceneToken);
            properties.put("scene", scene.getName());
        } catch (JSONException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
        Analytics.track(this, "compose", properties);

    }

    private void initChatText() {
        clearChatCursor();
        initSceneWordList();
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

    private void initCharacterWordList() {

        if (character.getIsBundleDownloaded()) {
            RealmResults<Scene> scenes = Scene.forCharacterToken(characterToken);

            final List<String> combinedWordList = new ArrayList<String>();
            for (Scene scene : scenes) {
                combinedWordList.add(scene.getWordList());
            }

            asyncWordListSetup(TextUtils.join(",",combinedWordList));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            debugView.setText("Downloading");

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
                    getWordListFragment().setWordTagWithDelay(targetWordTag, 500);
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
                    getWordListFragment().setWordTagWithDelay(targetWordTag, 500);
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
                    WordTag wordTag = getWordTagSelector().findWord(results.get(0), "next");
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
                    getWordListFragment().setWordTag(wordTag);
                }
            }
        });

    }

    // TEMP HACK to fix problem of:
    // before: "we are"
    // after: "weare" (about to delete we)
    // result: "weare" (we becomes merged with are)
    private void ensureWordTagCleanDelete(int start, int count) {
        if (count == 0 && editText.length() > 0 && editText.isImmediatelyAfterImageSpan()) {
            // backspace pressed
            // delete tagged word before it
            skipOnTextChangeCallback = true;
            int amountToDelete = start - editText.findTokenStart(start);
            editText.getText().delete(start - amountToDelete, start);
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


    // if invalid, dont proceed
    // if valid, autocomplete partial word with current selection

    public InputFilter handleSpaceKey() {
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.length() == 1 && source.charAt(0) == ' ' && editText.containsInvalidWord()) {
                    return "";
                } else if (source.length() == 1 && source.charAt(0) == ' ' && !editText.toString().trim().isEmpty() && !editText.getCurrentTokenWord().isEmpty()) {
                    // user press space in valid state
                    // user press spaced
                    List<String> filteredResults = editText.getFilteredResults();
                    if (!filteredResults.isEmpty()) {
                        // if valid wordTag completion is performed, we want original word list to show
                        editText.displayOriginalWordList();

                        String firstMatch = filteredResults.get(0);
                        WordTag wordTag = getWordTagSelector().findNextWord(firstMatch);
                        getWordListFragment().setWordTag(wordTag);

                        // autocompletion at work
                        return wordTag.toString().substring(editText.getCurrentTokenWord().length()) + " ";
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
            playMessageBtn.setEnabled(true);
//            playMessageBtn.setVisibility(View.VISIBLE);
        } else {
            playMessageBtn.setEnabled(false);
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
        isWordTagListContainerBlocked = true;

        skipOnTextChangeCallback = true;

        String toInsert = "";
        int cursorPosition = editText.getSelectionEnd();
        if (cursorPosition == editText.length()) {
            // insert end of sentence
            toInsert =  wordTag.toString() + " ";
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

        editText.getText().insert(cursorPosition, toInsert);
        editText.format();
        skipOnTextChangeCallback = false;

        focusOnWordTag(wordTag);

        getWordListFragment().setWordTag(wordTag);
        updatePlayMessageBtnState();
    }


    public void addWord(View view) {
        editText.getText().insert(editText.getSelectionStart(), " ");
    }

    private Runnable delayedWordPreviewPlayback;
    private Handler wordTagPlayHandler;

    @Override
    public void onWordTagChanged(final WordTag wordTag, int delayInMilliSeconds) {
        if (!wordTag.isFilled()) return;
        BardLogger.trace("onWordTagChanged: " + wordTag.toString());
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
        } else if (resultCode == RESULT_OK && requestCode == SHARE_REQUEST_CODE) {
            setResult(RESULT_OK);
            finish();
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


    private void displayInvalidWords() {
        if (invalidWords.size() > 0) {
            EventBus.getDefault().post(new InvalidWordEvent("Unavailable: " + TextUtils.join(",",invalidWords)));
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
    public void joinSegments(final List<String> wordTagList) {
        List<Segment> segments = Segment.buildFromWordTagList(wordTagList);
        final String outputFilePath = Storage.getMergedOutputFilePath();
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
                    lastMergedWordTagList = wordTagList;
                    onJoinSegmentsSuccess(outputFilePath);
                } else {
                    // report error
                    CrashReporter.logException(new Throwable(result));
                }
            }

        }).execute(cmd);

    }

    private void onJoinSegmentsSuccess(String outputFilePath) {
        // remember result (for sharing)
        playMessageBtn.setEnabled(true);

        hideKeyboard();
        trackGenerateBardVideo();
        playLocalVideo(outputFilePath);
        shareRepoBtn.setVisibility(View.VISIBLE);
        shareRepoIcon.setVisibility(View.VISIBLE);
        restoreOriginalVideoHeight();
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

    public void closeEditor(View view) {
        finish();
    }

    public void generateBardVideo(View view) throws IOException {
        List<String> wordTagList = new ArrayList<String>(Arrays.asList(editText.getText().toString().split("\\s+")));

        BardLogger.trace("[generateBardVideo] editText: '" + editText.getText() + "' wordTagList: " + wordTagList.toString());

        // replay last merge if nothing changed
        if (lastMergedWordTagList.toString().equals(wordTagList.toString())) {
            playLocalVideo(Storage.getMergedOutputFilePath());
            return;
        }

        if (editText.containsInvalidWord() && !isAllWordsTagged(wordTagList)) return;

        playMessageBtn.setEnabled(false);
        performActualMerge(wordTagList);
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


    // list of hashmap where key = word, value = tokenIndex
    private void performActualMerge(List<String> wordTagList) {
        progressBar.setVisibility(View.VISIBLE);

        Analytics.timeEvent(this, "generateBardVideo");
        joinSegments(wordTagList);
    }

    private void trackGenerateBardVideo() {

        JSONObject properties = new JSONObject();

        try {
            properties.put("wordTags", lastMergedWordTagList);
            properties.put("sceneToken", sceneToken);
            properties.put("scene", scene.getName());

//            properties.put("characterToken", characterToken);
//            properties.put("character", character.getName());
        } catch (JSONException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }

        Analytics.track(this, "generateBardVideo", properties);
        Analytics.sendQueuedEvents(this);
    }

    private WordTagSelector getWordTagSelector() {
        return  getWordListFragment().getWordTagSelector();
    }

    private WordListFragment getWordListFragment() {
        return this.wordListFragment;
    }

    private void playRemoteVideoAndDisplayThubmnail(String wordTagString) {

        if (!Helper.isConnectedToInternet()) {
            debugView.setText(R.string.no_network_connection);
            return;
        }

        String filePath = Storage.getCachedVideoFilePath(wordTagString);
        if (new File(filePath).exists()) {
            playWordTag(filePath);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            Storage.cacheVideo(wordTagString, new Storage.OnCacheVideoListener() {
                @Override
                public void onCacheVideoSuccess(String filePath) {
                    BardLogger.trace("video cached at " + filePath);
                    playWordTag(filePath);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCacheVideoFailure() {
                    Toast.makeText(ClientApp.getContext(),"Failed to download word preview", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void playWordTag(String filePath) {
        if (getWordListFragment() != null) {
            getWordListFragment().playPreview(filePath);
            isWordTagListContainerBlocked = false;
            if (lastClickedWordTagView != null) {
                lastClickedWordTagView.setEnabled(true);
            }
        }
    }


    public void playLocalVideo(String filePath) {
        progressBar.setVisibility(View.GONE);
        debugView.setText("");

        if (getWordListFragment() != null) {
            getWordListFragment().playPreview(filePath);
            isWordTagListContainerBlocked = false;
            if (lastClickedWordTagView != null) {
                lastClickedWordTagView.setEnabled(true);
            }
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
    public void onWordListFragmentReady() {
        initChatText();
        if (getWordListFragment() != null) {
            getWordListFragment().setOnWordTagChangedListener(this);
            getWordListFragment().setOnPreviewPlayerPreparedListener(this);
        }
    }

    @Override
    public void onPreviewPlayerPrepared() {
        progressBar.setVisibility(View.GONE);
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
            params.height = originalVideoHeight / 2;
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
                getWordListFragment().fixVideoAspectRatio();

                ViewTreeObserver obs = vpPagerContainer.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });

    }

    public void openSharing(View view) {
        Intent intent = new Intent(this, ShareEditorActivity.class);

        intent.putExtra("wordTags", TextUtils.join(",",lastMergedWordTagList));
        intent.putExtra("sceneToken", sceneToken);
        intent.putExtra("sceneName", scene.getName());
        startActivityForResult(intent, SHARE_REQUEST_CODE);
    }
}
