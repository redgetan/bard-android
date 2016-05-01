package com.roplabs.madchat.ui;

import android.content.Context;
import android.content.ContextWrapper;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MyActivity extends BaseActivity {

    public static final String EXTRA_MESSAGE = "com.roplabs.madchat.MESSAGE";
    public static final String EXTRA_REPO_TOKEN = "com.roplabs.madchat.REPO_TOKEN";
    public static final String EXTRA_VIDEO_URL = "com.roplabs.madchat.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.roplabs.madchat.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.roplabs.madchat.WORD_LIST";
    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int SETTINGS_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER = 4;

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
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

    private String repoToken;
    private String videoUrl;  // original url of video
    private String videoPath; // filepath of saved video
    private String wordList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mContext = this;

        editText = (MultiAutoCompleteTextView) findViewById(R.id.edit_message);
        packageDir = getExternalFilesDir(null).getAbsolutePath();

        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + "ffmpeg";
        debugView = (TextView) findViewById(R.id.display_debug);
        wordErrorView = (TextView) findViewById(R.id.display_word_error);
        videoView = (VideoView) findViewById(R.id.video_view);

        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);

        initVideoStorage();
        initFFmpeg();
        initWordIndex();
        initVideoPlayer();
        initChatText();
        initNavigationViewDrawer();
        initAnalytics();
    }

    private void initVideoStorage() {
        moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .getAbsolutePath() + "/" +  getResources().getString(R.string.app_name) + "/";

        File moviesDirFile = new File(moviesDir);

        if (!moviesDirFile.exists()) {
          moviesDirFile.mkdirs();
        }
    }

    public void initFFmpeg() {
        String binary = "ffmpeg";

        if (!(new File(ffmpegPath)).exists()) {

            // copy ffmpeg binary from assets folder to /data/data/com.roplabs.*
            try {
                InputStream inputStream = getAssets().open(binary);
                File file = Helper.getSafeOutputFile(applicationDir, binary);
                Helper.writeToFile(inputStream, file);
            } catch (IOException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }

            Helper.runCmd(new String[] { "/system/bin/chmod", "744", ffmpegPath});
        }
    }

    public void initAnalytics() {
        Tracker mTracker = ((ClientApp) getApplication()).getDefaultTracker();
        mTracker.setScreenName("ChatActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }


    public void initWordIndex() {
        RealmResults<Index> indexResults = Index.findAll();
        if (indexResults.size() == 0) {
            try {
                populateWordIndex("smosh_index.json");
                populateWordIndex("donald_trump_index.json");
                populateWordIndex("kevin_hart_index.json");
                populateWordIndex("emma_watson_index.json");

                setDefaultIndex();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDefaultIndex() {
        Setting.setCurrentIndexToken(this,Index.findFirst().getToken());
    }

    public void populateWordIndex(String indexFileName) throws IOException, JSONException {
        AssetManager assetManager = getAssets();
        InputStream input = assetManager.open(indexFileName);

        JSONObject obj = new JSONObject(FileManager.readInputStream(input));

        Index.create(obj.getString("token"),
                     obj.getString("name"),
                     obj.getString("description"),
                     obj.getString("wordList"));
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
            Log.e("madchat", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initAutocompleteWords() {
//        String[] words = new String[] {
//                "the", "this", "time", "tree", "tell", "tod", "take","tall", "tam", "taker", "taken",
//                "tad", "tar", "tame", "tamer", "tap", "tape", "tale","tail", "tarzan", "tan",
//                "hello", "world", "i", "am", "funny", "fun", "food","in", "what", "are", "you"
//        };
        String[] words = Index.forToken(Setting.getCurrentIndexToken(this))
                              .getWordList()
                              .split(",");
        TrieAdapter<String> adapter =
                new TrieAdapter<String>(this, android.R.layout.simple_list_item_1, words);
        editText.setAdapter(adapter);
        editText.setTokenizer(new SpaceTokenizer());
    }

    private void initNavigationViewDrawer() {
// Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.profile_header)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName("Mike Penz").withEmail("mikepenz@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile))
                )
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.create_string).withIdentifier(CREATE_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_create_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.choose_character_string).withIdentifier(CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.abc_ic_star_black_36dp),
                        new PrimaryDrawerItem().withName(R.string.my_projects_string).withIdentifier(MY_PROJECTS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_inbox_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.settings_string).withIdentifier(SETTINGS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_settings_black_24dp)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        Intent intent;

                        switch ((int) drawerItem.getIdentifier()) {
                            case CREATE_DRAWER_ITEM_IDENTIFIER:
                                Toast.makeText(getApplicationContext(),"Create",Toast.LENGTH_SHORT).show();
                                break;
                            case CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(mContext, IndexActivity.class);
                                startActivity(intent);
                                break;
                            case MY_PROJECTS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(mContext, RepoListActivity.class);
                                startActivity(intent);
                                break;
                            case SETTINGS_DRAWER_ITEM_IDENTIFIER:
                                Toast.makeText(getApplicationContext(),"Settings",Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }

                        // allows drawer to close
                        return false;
                    }
                })
                .build();

    }


    // http://stackoverflow.com/a/28939113

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
   }

    // http://developer.android.com/guide/topics/ui/menus.html
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(mDrawerLayout.getChildAt(1)))
                mDrawerLayout.closeDrawers();
            else {
                mDrawerLayout.openDrawer(mDrawerLayout.getChildAt(1));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
