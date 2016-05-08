package com.roplabs.madchat.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.roplabs.madchat.ClientApp;
import com.roplabs.madchat.R;
import com.roplabs.madchat.api.MadchatClient;
import com.roplabs.madchat.events.VideoDownloadEvent;
import com.roplabs.madchat.events.VideoQueryEvent;
import com.roplabs.madchat.models.*;
import com.roplabs.madchat.util.*;
import io.realm.RealmResults;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class MimicActivity extends BaseActivity {

    public static final String EXTRA_MESSAGE = "com.roplabs.madchat.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.madchat.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.madchat.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.madchat.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.madchat.WORD_LIST";

    private Context mContext;

    private MultiAutoCompleteTextView editText;
    private TextView debugView;
    private TextView wordErrorView;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private String packageDir;
    private String applicationDir;
    private String moviesDir;
    private String ffmpegPath;
    private ProgressBar progressBar;

    private MenuItem shareMenuItem;
    private boolean isVideoReady = false;
    private boolean isVideoBeingTouched = false;
    private Handler mHandler = new Handler();

    private NavigationView navigationView;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

    private Trie<String, String> wordTrie;
    private String repoToken;
    private String videoUrl;  // original url of video
    private String videoPath; // filepath of saved video
    private String wordList;
    private String[] availableWordList;
    Set<String> invalidWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mimic);
        mContext = this;

        editText = (MultiAutoCompleteTextView) findViewById(R.id.edit_message);
        packageDir = getExternalFilesDir(null).getAbsolutePath();

        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + "ffmpeg";
        debugView = (TextView) findViewById(R.id.display_debug);
        wordErrorView = (TextView) findViewById(R.id.display_word_error);
        videoView = (VideoView) findViewById(R.id.video_view);

        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        invalidWords = new HashSet<String>();

        Intent intent = getIntent();
        String indexName = intent.getStringExtra("indexName");
        setTitle(indexName);

        initVideoStorage();
        initVideoPlayer();
        initChatText();
        initAnalytics();

        showKeyboardOnStartup();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    private void showKeyboardOnStartup() {
        if (editText.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
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


    private void initVideoPlayer() {
//        videoView.setMediaController(new MediaController(this));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.setBackgroundColor(Color.TRANSPARENT);
                isVideoReady = true;
                mediaPlayer = mp;
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // http://stackoverflow.com/a/14163267

                if (!isVideoReady) return false;

                mediaPlayer.seekTo(0);
                mediaPlayer.start();

                return false;
            }
        });
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
                wordTrie = buildWordTrie();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                initMultiAutoComplete();
                progressBar.setVisibility(View.GONE);
                debugView.setText("");
            }

        }).execute();
    }

    private Trie<String, String> buildWordTrie() {
        Trie<String, String> trie = new PatriciaTrie<String>();
        for (String word : availableWordList ) {
            trie.put(word, null);
        }

        return trie;
    }

    private void initMultiAutoComplete() {
        TrieAdapter<String> adapter =
                new TrieAdapter<String>(this, android.R.layout.simple_list_item_1, availableWordList, wordTrie);
        editText.setAdapter(adapter);
        editText.setTokenizer(new SpaceTokenizer());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int endPos = editText.getSelectionEnd();
                int startPos = (new SpaceTokenizer()).findTokenStart(s, endPos);

                Boolean isWordComplete = (startPos == endPos) && (startPos != 0);
                if (isWordComplete) {
                    notifyUserOnUnavailableWord();
                } else {
                    updateInvalidWords();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateInvalidWords() {
        Editable text = editText.getText();
        List<String> words = Arrays.asList(text.toString().split(" "));
        invalidWords.retainAll(words);

        displayInvalidWords();
    }

    private void notifyUserOnUnavailableWord() {
        Editable text = editText.getText();
        String[] words = text.toString().split(" ");

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
            wordErrorView.setText("Words not available: " + TextUtils.join(",",invalidWords));
        } else {
            wordErrorView.setText("");
        }
    }

    // http://stackoverflow.com/a/28939113

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            View v = getCurrentFocus();
//            if ( v instanceof EditText) {
//                Rect outRect = new Rect();
//                v.getGlobalVisibleRect(outRect);
//                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
//                    v.clearFocus();
//                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
//                }
//            }
//        }
//        return super.dispatchTouchEvent( event );
//   }


    @Override
    protected void onStop() {
        super.onStop();
        hideKeyboard();
    }

    // use ffmpeg binary to concat videos hosted in cloudfront (run in background thread)
    public void joinSegments(List<Segment> segments) {
        final String outputFilePath = getJoinedOutputFilePath(segments);
        String[] cmd = buildJoinSegmentsCmd(segments, outputFilePath);

        (new AsyncTask<String[], Integer, String>() {
            @Override
            protected String doInBackground(String[]... cmds) {
                return Helper.runCmd(cmds[0]);
            }

            @Override
            protected void onPostExecute(String result) {
                // check if file was created
                if ((new File(outputFilePath)).exists()) {
                    playLocalVideo(outputFilePath);
                } else {
                    // report error
                    Crashlytics.logException(new Throwable(result));
                }
            }
        }).execute(cmd);

    }


    public String[] buildJoinSegmentsCmd(List<Segment> segments, String outputFilePath) {
        List<String> cmd = new ArrayList<String>();

        cmd.add(ffmpegPath);

        for (Segment segment : segments) {
            cmd.add("-i");
            cmd.add(segment.getSourceUrl());
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
            progressBar.setVisibility(View.VISIBLE);

            String message = editText.getText().toString();
            MadchatClient.getQuery(message, Setting.getCurrentIndexToken(this));
        } else {
            // display error
            debugView.setText(R.string.no_network_connection);
            return;
        }

//        Intent intent = new Intent(this, DisplayMessageActivity.class);
//        EditText editText = (EditText) findViewById(R.id.edit_message);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent);
    }

    public void playLocalVideo(String filePath) {
        progressBar.setVisibility(View.GONE);
        debugView.setText("");

        videoView.setVideoPath(filePath);
        videoView.requestFocus();
        videoView.start();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(VideoQueryEvent event) {
//        this.repoToken = event.token;
//        this.wordList = event.wordList;
//        this.videoUrl = event.videoUrl;

        if (event.error != null) {
            progressBar.setVisibility(View.GONE);
            debugView.setText(event.error);
        } else {
            joinSegments(event.segments);
        }
    }

    @Subscribe
    public void onEvent(VideoDownloadEvent event) {
        progressBar.setVisibility(View.GONE);

        if (event.error != null) {
            debugView.setText(event.error);
            Crashlytics.logException(new Throwable(event.error));
        } else {
            // save MADs by default
            this.videoPath = event.videoPath;

            Repo.create(repoToken, videoUrl, videoPath, wordList, Calendar.getInstance().getTime());
            setShareProvider();

            videoView.setVideoPath(this.videoPath);
            videoView.requestFocus();
            videoView.start();
        }

    }

    public void setShareProvider() {
        // http://stackoverflow.com/a/21630571/
        ShareActionProvider mShareActionProvider = new ShareActionProvider(this);
        MenuItemCompat.setActionProvider(this.shareMenuItem, mShareActionProvider);
        mShareActionProvider.setShareIntent(getShareIntent());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);

        // Locate MenuItem with ShareActionProvider
        this.shareMenuItem = menu.findItem(R.id.menu_item_share);

        return true;
    }

    public Intent getShareIntent() {
        Uri videoUri = Uri.fromFile(new File(this.videoPath));
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }


}
