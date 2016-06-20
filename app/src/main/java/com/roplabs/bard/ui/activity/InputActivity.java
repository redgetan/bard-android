package com.roplabs.bard.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
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
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.*;
import com.roplabs.bard.models.*;
import com.roplabs.bard.ui.fragment.VideoResultFragment;
import com.roplabs.bard.ui.fragment.WordListFragment;
import com.roplabs.bard.ui.widget.InputViewPager;
import com.roplabs.bard.ui.widget.WordsAutoCompleteTextView;
import com.roplabs.bard.util.*;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class InputActivity extends BaseActivity implements WordListFragment.OnWordListViewReadyListener {

    public static final String EXTRA_MESSAGE = "com.roplabs.bard.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.bard.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.bard.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.bard.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.bard.WORD_LIST";

    private Context mContext;
    private InputViewPager vpPager;
    private FrameLayout vpPagerContainer;
    private TextView debugView;
    private WordsAutoCompleteTextView editText;
    private TextView wordErrorView;
    private String packageDir;
    private String applicationDir;
    private String moviesDir;
    private String ffmpegPath;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private SmartFragmentStatePagerAdapter adapterViewPager;
    private LinearLayout editTextContainer;

    private MenuItem shareMenuItem;
    private Handler mHandler = new Handler();

    private NavigationView navigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

    private Trie<String, String> wordTrie;
    private LinkedList<WordTag> wordTagList;

    private Repo repo;
    private String[] availableWordList;
    private String[] uniqueWordList;
    Set<String> invalidWords;
    private String indexName;
    private ImageView sendMessageBtn;

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
        wordErrorView = (TextView) findViewById(R.id.display_word_error);

        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        vpPagerContainer = (FrameLayout) findViewById(R.id.vp_pager_container);
        invalidWords = new HashSet<String>();
        wordTagList = new LinkedList<WordTag>();
        editTextContainer = (LinearLayout) findViewById(R.id.bard_text_entry);
        sendMessageBtn = (ImageView) findViewById(R.id.send_message_btn);

        Intent intent = getIntent();
        indexName = intent.getStringExtra("indexName");
        setTitle(indexName);

        initVideoStorage();
        initAnalytics();
        initViewPager();
        initChatText();
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
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) vpPagerContainer.getLayoutParams();

                if (position == 0) {
                    params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                } else if (position == 1) {
                    params.height = 200;
//                    params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                }

                vpPagerContainer.setLayoutParams(params);
                vpPagerContainer.requestLayout();
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


    public void onWordListViewReady(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.editText.setRecyclerView(recyclerView);
    }

    private void initChatText() {
        clearChatCursor();
        initAutocompleteWords();
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

    private void initAutocompleteWords() {
        progressBar.setVisibility(View.VISIBLE);
        debugView.setText("Initializing Available Word List");
        final Context context = this;

        (new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                availableWordList = Index.forToken(Setting.getCurrentIndexToken(context)).getWordList().split(",");

                uniqueWordList = buildUniqueWordList();
                wordTrie = buildWordTrie();

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                initFindInPage();
                initMultiAutoComplete();
                showKeyboard();
                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }

        }).execute();
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
        String[] words = new String[] { "shit", "today", "is", "isnt", "it", "hot", "cant", "you", "event", "smell", "what", "the", "rock", "is", "cooking", "extravaganza" };
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
        editText.setAutoCompleteWords(wordTrie);
        editText.setTokenizer(new SpaceTokenizer());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleUnavailableWords(s, start);
                updateWordTagList(s, start);
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
                        WordTag wordTag = wordTagList.get(tokenIndex);
                        EventBus.getDefault().post(new PreviewWordEvent(wordTag.toString()));
                    }
                }
            }
        });
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

    private void updateWordTagList(CharSequence s, int start) {
        String character = editText.getAddedChar(start);
        boolean isLeaderPressed = character.equals(" ");
        String lastWord = editText.getLastWord();
        int tokenIndex = editText.getTokenIndex();

        if (isLeaderPressed) {
            WordTag wordTag = wordTagList.get(tokenIndex);
            if (wordTag.tag.isEmpty() && !lastWord.isEmpty()) {
                String wordTagString = getWordTagSelector().findNextWord(lastWord);
                if (!wordTagString.isEmpty()) {
                    String tag = wordTagString.split(":")[1];
                    wordTag.tag = tag;

                    getWordListFragment().queryWordPreview(wordTag.toString());
                }
            }
        } else {
            int tokenCount = editText.getTokenCount();
            if (tokenCount > wordTagList.size()) {
                // ADD wordTag (when token count increases)
                WordTag wordTag = new WordTag(lastWord);
                wordTagList.add(tokenIndex, wordTag);
            } else if (tokenCount < wordTagList.size()) {
                // DELETE wordTag (when token count decreases)
                wordTagList.remove(tokenIndex + 1);
            } else {
                // UPDATE wordTag (when word changed)
                WordTag wordTag = wordTagList.get(tokenIndex);
                if (!wordTag.word.equals(lastWord)) {
                    wordTag.tag = "";
                    wordTag.word = lastWord;
                }

            }

        }
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


    public void sendMessage(View view) throws IOException {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if (addMissingWordTag()) {
                progressBar.setVisibility(View.VISIBLE);

                showVideoResultFragment();
                vpPager.setAllowedSwipeDirection(InputViewPager.SwipeDirection.none);

                String message = getWordMessage();
                BardClient.getQuery(message, Setting.getCurrentIndexToken(this), false);
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
                String wordTagString = getWordTagSelector().findNextWord(wordTag.word);
                if (wordTagString.isEmpty()) {
                    return false;
                } else {
                    wordTag.tag = wordTagString.split(":")[1];
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

        vpPager.setAllowedSwipeDirection(InputViewPager.SwipeDirection.all);
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
                if (getWordListFragment() != null) {
                    getWordListFragment().playPreview(event.segments.get(0));
                }
            } else {
                VideoDownloader.fetchSegments(event.segments);
            }
        }
    }

    private void showVideoResultFragment() {
        editText.setEnabled(false);
        sendMessageBtn.setVisibility(View.GONE);

        if (vpPager.getCurrentItem() != 1) {
            vpPager.setCurrentItem(1, true);
        }

        setTitle(R.string.share);
        if (shareMenuItem != null) shareMenuItem.setVisible(true);
    }

    private void showWordListFragment() {
        editText.setEnabled(true);
        sendMessageBtn.setVisibility(View.VISIBLE);

        if (vpPager.getCurrentItem() != 0) {
            vpPager.setCurrentItem(0, true);
        }

        setTitle(indexName);
        if (shareMenuItem != null) shareMenuItem.setVisible(false);

        showKeyboard();
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
    public void onEvent(ReplaceWordEvent event) {
        int tokenIndex = editText.getTokenIndex();
        WordTag wordTag = wordTagList.get(tokenIndex);
        wordTag.tag = event.wordTagString.split(":")[1];
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

}
