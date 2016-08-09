package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.*;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.InputPagerAdapter;
import com.roplabs.bard.adapters.SmartFragmentStatePagerAdapter;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.*;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.ui.fragment.VideoResultFragment;
import com.roplabs.bard.ui.fragment.WordListFragment;
import com.roplabs.bard.ui.widget.InputViewPager;
import com.roplabs.bard.ui.widget.WordsAutoCompleteTextView;
import com.roplabs.bard.util.*;
import io.realm.Realm;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class BardEditorActivity extends BaseActivity implements WordListFragment.OnReadyListener {

    public static final String EXTRA_MESSAGE = "com.roplabs.bard.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.bard.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.bard.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.bard.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.bard.WORD_LIST";

    private Context mContext;
    private RelativeLayout inputContainer;
    private InputViewPager vpPager;
    private FrameLayout vpPagerContainer;
    private TextView debugView;
    private WordsAutoCompleteTextView editText;
    private String packageDir;
    private String applicationDir;
    private String moviesDir;
    private String ffmpegPath;
    public ProgressBar progressBar;
    private RecyclerView recyclerView;
    private SmartFragmentStatePagerAdapter adapterViewPager;
    private LinearLayout editTextContainer;
    private ImageView currentImageView;
    private ImageView lastImageView;
    private int currentTokenIndex;

    private MenuItem shareMenuItem;
    private Handler mHandler = new Handler();

    private NavigationView navigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

    private Trie<String, String> wordTrie;
    private LinkedList<WordTag> wordTagList;
    private boolean skipOnTextChangeCallback;

    private String characterToken;
    private String sceneToken;
    private Character character;
    private Scene scene;
    private Repo repo;
    private List<String> availableWordList;
    private String[] uniqueWordList;
    Set<String> invalidWords;
    private Button playMessageBtn;
    private ImageView showKeyboardBtn;
    private ImageView showWordChoiceBtn;
    private LinearLayout previewTimeline;
    private FrameLayout previewTimelineContainer;
    private WordListAdapter.ViewHolder lastViewHolder;

    ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        mContext = this;

        debugView = (TextView) findViewById(R.id.display_debug);

        editText = (WordsAutoCompleteTextView) findViewById(R.id.edit_message);
        packageDir = getExternalFilesDir(null).getAbsolutePath();

        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + "ffmpeg";

        inputContainer = (RelativeLayout) findViewById(R.id.input_container);
        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        vpPagerContainer = (FrameLayout) findViewById(R.id.vp_pager_container);
        invalidWords = new HashSet<String>();
        wordTagList = new LinkedList<WordTag>();
        editTextContainer = (LinearLayout) findViewById(R.id.bard_text_entry);
        playMessageBtn = (Button) findViewById(R.id.play_message_btn);
        showKeyboardBtn = (ImageView) findViewById(R.id.show_keyboard_btn);
        showWordChoiceBtn = (ImageView) findViewById(R.id.show_word_choice_btn);
        previewTimeline = (LinearLayout) findViewById(R.id.preview_timeline);
        previewTimelineContainer = (FrameLayout) findViewById(R.id.preview_timeline_container);
        recyclerView = (RecyclerView) findViewById(R.id.word_list_dictionary);


        Intent intent = getIntent();
        characterToken = intent.getStringExtra("characterToken");
        sceneToken = intent.getStringExtra("sceneToken");
        character  = Character.forToken(characterToken);
        scene      = Scene.forToken(sceneToken);

        initPreviewTimeline();
        initVideoStorage();
        initAnalytics();
        initViewPager();
        setCharacterOrSceneTitle();
    }

    private void setCharacterOrSceneTitle() {
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        if (sceneToken.length() != 0) {
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


    private void initWordTagView() {
        editText.setEnableAutocomplete(false);
        editText.setRecyclerView(recyclerView);

        WordListAdapter adapter = new WordListAdapter(this, availableWordList);
        adapter.setIsWordTagged(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new WordsLayoutManager(ClientApp.getContext()));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                if (getWordTagSelector().getCurrentScrollPosition() != -1) {
//                    WordListAdapter.ViewHolder viewHolder = (WordListAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(getWordTagSelector().getCurrentScrollPosition());
//                    viewHolder.tagView.setBackgroundColor(Color.YELLOW);
//                    if (lastViewHolder != null) lastViewHolder.tagView.setBackgroundColor(Color.TRANSPARENT);
//                    lastViewHolder = viewHolder;
//                    getWordTagSelector().setCurrentScrollPosition(-1);
//                }
                super.onScrolled(recyclerView, dx, dy);
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
        moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .getAbsolutePath() + "/" +  getResources().getString(R.string.app_name) + "/";

        File moviesDirFile = new File(moviesDir);

        if (!moviesDirFile.exists()) {
            moviesDirFile.mkdirs();
        }
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
            Log.e("Mimic", e.getMessage());
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

    private void onWordListAvailable() {
        initFindInPage();
        initWordTagView();
        initMultiAutoComplete();
        progressBar.setVisibility(View.GONE);
        debugView.setText("");
    }

    private void initSceneWordList() {
        if (scene.getWordList() == null) {
            Call<Scene> call = BardClient.getBardService().getSceneWordList(characterToken, sceneToken);
            call.enqueue(new Callback<Scene>() {
                @Override
                public void onResponse(Call<Scene> call, Response<Scene> response) {
                    Scene remoteScene = response.body();
                    String wordList = remoteScene.getWordList();

                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    scene.setWordList(wordList);
                    realm.commitTransaction();

                    addWordListToDictionary(wordList);
                    onWordListAvailable();
                }

                @Override
                public void onFailure(Call<Scene> call, Throwable t) {
                    progressBar.setVisibility(View.VISIBLE);
                    debugView.setText("");
                    Toast.makeText(getApplicationContext(), "Failed to load word list", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            addWordListToDictionary(scene.getWordList());
            onWordListAvailable();
        }
    }

    private void initCharacterWordList() {

    }

    private void addWordListToDictionary(String wordList) {
        availableWordList = new ArrayList<String>(Arrays.asList(wordList.split(",")));
        uniqueWordList = buildUniqueWordList();
        wordTrie = buildWordTrie();
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

    public void initFindInPage() {
        getWordListFragment().initFindInPage(availableWordList);
    }

    private void initMultiAutoComplete() {

//        TrieAdapter<String> adapter =
//                new TrieAdapter<String>(this, android.R.layout.simple_list_item_1, availableWordList, wordTrie);
//        editText.setAutoCompleteWords(wordTrie);
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
                if (editText.getTokenizer() != null) {
                    int tokenIndex = editText.getTokenIndex();
                    if (tokenIndex < wordTagList.size()) {
                        setCurrentImageView((ImageView) previewTimeline.getChildAt(tokenIndex));
                        currentTokenIndex = tokenIndex;
                        WordTag wordTag = wordTagList.get(tokenIndex);
                        getWordListFragment().setWordTag(wordTag);
                    }
                }
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (showKeyboardBtn.isShown()) {
                    showKeyboardBtn.setVisibility(View.GONE);
                    showWordChoiceBtn.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }

                return false;
            }
        });
    }

    private void updatePlayAllBtnState() {
        if (getTimelineEnabledImageViewCount() > 0) {
            playMessageBtn.setEnabled(true);
        } else {
            playMessageBtn.setEnabled(false);
        }
    }

    private void handleUnavailableWords(CharSequence s, int start) {
        int endPos = editText.getSelectionEnd();
        int startPos = editText.getTokenizer().findTokenStart(s, endPos);

        Boolean isWordComplete = (startPos == endPos) && (startPos != 0);
        if (isWordComplete) {
            notifyUserOnUnavailableWord();
        } else {
            updateInvalidWords();
        }
    }

    @Subscribe
    public void onEvent(TagClickEvent event) throws IOException {
        // insert word in current character
        //   - wordTagList
        //   - editText
        skipOnTextChangeCallback = true;
        editText.insertText(event.wordTag.word + " ");
        skipOnTextChangeCallback = false;

        currentTokenIndex = editText.getTokenIndex();
        setCurrentImageView((ImageView) previewTimeline.getChildAt(currentTokenIndex));

        wordTagList.add(currentTokenIndex,event.wordTag);
        getWordListFragment().setWordTag(event.wordTag);
    }


    private void updateWordTagList(CharSequence s, int start) {
        String character = editText.getAddedChar(start);
        String nextCharacter = editText.getNextChar(s, start);
        boolean isLeaderPressed = character.equals(" ");
        boolean isBackspacePressed = character.equals("");
        String lastWord = editText.getLastWord();
        int tokenIndex = editText.getTokenIndex();

        // when adding character, always make sure there's a space right after, if not, add one (to preserve token count)
        if (!isBackspacePressed && !nextCharacter.equals("") && !nextCharacter.equals(" ")) {
            skipOnTextChangeCallback = true;
            editText.insertText(" ");
            editText.setSelection(start + 1);
            skipOnTextChangeCallback = false;
        }

        if (isLeaderPressed && !nextCharacter.equals(" ") && !nextCharacter.equals("")) {
            lastWord = editText.getPrevWord(start);
            tokenIndex--;
        }
        currentTokenIndex = tokenIndex;
        WordTag wordTag = null;

        if (isLeaderPressed && (wordTagList.size() > tokenIndex)) {
            if (editText.getTokenCount() > wordTagList.size()) {
                // ADD wordTag (when token count increases)
                wordTag = new WordTag(lastWord);
                wordTagList.add(tokenIndex, wordTag);
            } else if (!lastWord.isEmpty()) {
                wordTag = wordTagList.get(tokenIndex);
            }

            if (wordTag != null && wordTag.tag.isEmpty() && !lastWord.isEmpty()) {
                WordTag targetWordTag = getWordTagSelector().findNextWord(lastWord);
                if (targetWordTag != null) {
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
                    progressBar.setVisibility(View.VISIBLE);
                    getWordListFragment().setWordTag(targetWordTag);
                }
            }
        } else {
            int tokenCount = editText.getTokenCount();
            if (tokenCount > wordTagList.size()) {
                // ADD wordTag (when token count increases)
                wordTag = new WordTag(lastWord);
                wordTagList.add(tokenIndex, wordTag);
            } else if (tokenCount < wordTagList.size()) {
                // DELETE wordTag (when token count decreases)
                ImageView imageView;
                String nextWordInWordTagList = "";
                String nextImmediateWord = editText.getText().toString().subSequence(start, editText.length()).toString().trim();
                if (tokenIndex + 1 < wordTagList.size()) {
                    nextWordInWordTagList = wordTagList.get(tokenIndex + 1).word;
                } else {
                    nextWordInWordTagList = "";
                }
                if (nextImmediateWord.equals(nextWordInWordTagList)) {
                    wordTagList.remove(tokenIndex);
                    imageView = (ImageView) previewTimeline.getChildAt(tokenIndex);
                } else {
                    wordTagList.remove(tokenIndex + 1);
                    imageView = (ImageView) previewTimeline.getChildAt(tokenIndex + 1);
                }
                if (imageView != null) {
                    previewTimeline.removeView(imageView);
                    previewTimeline.addView(createPreviewImageView(null));
                    updatePlayAllBtnState();
                }

            } else {
                // UPDATE wordTag (when word changed)
                String nextImmediateWord = editText.getText().toString().subSequence(start, editText.length()).toString().trim();
                WordTag nextWordTag = wordTagList.get(tokenIndex);
                String nextWordInWordTagList;

                if (nextWordTag != null) {
                    nextWordInWordTagList = nextWordTag.word;
                } else {
                    nextWordInWordTagList = ""; 
                }

                if (nextImmediateWord.equals(nextWordInWordTagList)) {
                    // dont change word tag (might be used again)
                } else {
                    wordTag = wordTagList.get(tokenIndex);
                    if (!wordTag.word.equals(lastWord)) {
                        wordTag.tag = "";
                        wordTag.word = lastWord;
                    }
                }

            }

        }
    }

    public void showKeyboard(View view) {
        showKeyboardBtn.setVisibility(View.GONE);
        showWordChoiceBtn.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        this.showKeyboard();
    }

    public void showWordChoice(View view) {
        showKeyboardBtn.setVisibility(View.VISIBLE);
        showWordChoiceBtn.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        this.hideKeyboard();
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

    private String getWordMessage() {
        TextUtils.join(" ", wordTagList);
        List<String> wordTagStringList = new ArrayList<String>();

        for (WordTag wordTag : wordTagList) {
            wordTagStringList.add(wordTag.toString());
        }

        return TextUtils.join(" ", wordTagStringList);
    }

    private void updateInvalidWords() {
        Editable text = editText.getText();
        List<String> words = Arrays.asList(text.toString().trim().split("\\s+"));
        invalidWords.retainAll(words);

        displayInvalidWords();
    }

    private void notifyUserOnUnavailableWord() {
        Editable text = editText.getText();
        String[] words = text.toString().trim().split("\\s+");

        invalidWords.clear();

        for (String word : words) {
            if (!wordTrie.containsKey(word)) {
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
        final String outputFilePath = getJoinedOutputFilePath(segments);
        final String wordList = getWordListFromSegments(segments);
        String[] cmd = buildJoinSegmentsCmd(segments, outputFilePath);
        final long startTime = System.currentTimeMillis();
        Log.e("Mimic", TextUtils.join(",",cmd));

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
                    Log.e("Mimic", String.valueOf(endTime - startTime) + " seconds" );
                    onJoinSegmentsSuccess(outputFilePath, wordList);
                } else {
                    // report error
                    Crashlytics.logException(new Throwable(result));
                }
            }

        }).execute(cmd);

    }

    private void onJoinSegmentsSuccess(String outputFilePath, String wordList) {
        repo = saveRepo(outputFilePath, wordList);
        shareMenuItem.setVisible(true);
        playLocalVideo(outputFilePath);
    }

    private String getWordListFromSegments(List<Segment> segments) {
        List<String> list = new ArrayList<String>();
        for (Segment segment: segments) {
            list.add(segment.getWord());
        }
        return TextUtils.join(" ", list);
    }

    private Repo saveRepo(String videoPath, String wordList) {
        return Repo.create(null, null, videoPath, wordList, Calendar.getInstance().getTime());
    }


    public String[] buildJoinSegmentsCmd(List<Segment> segments, String outputFilePath) {
        List<String> cmd = new ArrayList<String>();

        cmd.add(ffmpegPath);

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

    public String getJoinedOutputFilePath(List<Segment> segments) {
        List<String> wordList = new ArrayList<String>();

        for (Segment segment: segments) {
            wordList.add(segment.getWord());
        }

        return moviesDir + Helper.getTimestamp() + ".mp4";
    }


    public void playMessage(View view) throws IOException {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if (addMissingWordTag()) {
                progressBar.setVisibility(View.VISIBLE);

                showVideoResultFragment();

                String message = getWordMessage();
            } else {
                notifyUserOnUnavailableWord();
            }
        } else {
            // display error
            debugView.setText(R.string.no_network_connection);
            return;
        }
    }

    // return false if wordtag missing and unable to find match. true otherwise
    public boolean addMissingWordTag() {
        for (WordTag wordTag : wordTagList) {
            if (wordTag.tag.isEmpty()) {
                WordTag targetWordTag = getWordTagSelector().findNextWord(wordTag.word);
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

    @Subscribe
    public void onEvent(VideoQueryEvent event) {
        if (event.error != null) {
            progressBar.setVisibility(View.GONE);
            debugView.setText(event.error);
        } else {
            if (event.isPreview) {
                progressBar.setVisibility(View.GONE);
                if (getWordListFragment() != null) {
                    getWordListFragment().playPreview(event.segments.get(0));
                }
            } else {
                VideoDownloader.fetchSegments(event.segments);
            }
        }
    }

    private void showVideoResultFragment() {
        editTextContainer.setVisibility(View.GONE);
        previewTimelineContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        playMessageBtn.setVisibility(View.GONE);

        if (vpPager.getCurrentItem() != 1) {
            vpPager.setCurrentItem(1, true);
        }

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.share);

        if (shareMenuItem != null) shareMenuItem.setVisible(true);
    }

    private void showWordListFragment() {
        editTextContainer.setVisibility(View.VISIBLE);
        previewTimelineContainer.setVisibility(View.VISIBLE);
        if (showKeyboardBtn.isShown()) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        playMessageBtn.setVisibility(View.VISIBLE);

        if (vpPager.getCurrentItem() != 0) {
            vpPager.setCurrentItem(0, true);
        }

        setCharacterOrSceneTitle();
        if (shareMenuItem != null) shareMenuItem.setVisible(false);
    }

    @Subscribe
    public void onEvent(VideoDownloadEvent event) {
        if (event.error != null) {
            setVideoError(event.error);
            progressBar.setVisibility(View.GONE);
            Crashlytics.logException(new Throwable(event.error));
        } else if (event.segments != null) {
            joinSegments(event.segments);
        }
    }

    @Subscribe
    public void onEvent(FetchWordClipEvent event) {
        progressBar.setVisibility(View.VISIBLE);
        int tokenIndex = editText.getTokenIndex();
        WordTag wordTag = wordTagList.get(tokenIndex);
        wordTag.tag = event.wordTag.tag;
        recyclerView.scrollToPosition(event.wordTag.position);
    }

    private void setVideoError(String error) {
        debugView.setText(error);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mimic, menu);
        shareMenuItem = menu.findItem(R.id.menu_item_share);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (vpPager.getCurrentItem() == 0) {
                    // wordListFragment
                    finish();
                } else if (vpPager.getCurrentItem() == 1) {
                    showWordListFragment();
                }

                return true;
            case R.id.menu_item_share:
                startActivity(getRepoShareIntent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Intent getRepoShareIntent() {
        Uri videoUri = Uri.fromFile(new File(this.repo.getFilePath()));
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
    }


    // click on word in edittext -> getTokenIndex -> PreviewWordEvent -> WordListFragment#queryWordPreview -> BardClient.getQuery -> VideoQueryEvent -> WordListFragment#playPreview -> onVideoThumbnailChanged

    @Override
    public void onVideoThumbnailChanged(Bitmap bitmap) {
        if (currentImageView != null) {
            currentImageView.setImageBitmap(bitmap);
            currentImageView.setEnabled(true);
            updatePlayAllBtnState();
        } else {
            currentImageView = createPreviewImageView(bitmap);
            previewTimeline.addView(currentImageView, currentTokenIndex);
        }

        currentImageView.setSelected(true);
    }

    public ImageView createPreviewImageView(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setEnabled(false);
        }

        imageView.setBackgroundResource(R.drawable.selector_preview_image);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentImageView((ImageView) v);
                v.setSelected(true);

                int tokenIndex = previewTimeline.indexOfChild(v);

                HashMap<String,Integer> result = SpaceTokenizer.findStartStopOfNthToken(editText.getText(), tokenIndex);
                if (result.get("start") >= 0 && result.get("stop") >= 0) {
                    editText.setSelection(result.get("start"), result.get("stop"));
                }

                if (tokenIndex < wordTagList.size()) {
                    WordTag wordTag = wordTagList.get(tokenIndex);
                    getWordListFragment().setWordTag(wordTag);
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
}
