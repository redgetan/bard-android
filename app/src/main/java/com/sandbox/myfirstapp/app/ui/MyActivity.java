package com.sandbox.myfirstapp.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.orm.SugarRecord;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.api.MadchatClient;
import com.sandbox.myfirstapp.app.events.VideoDownloadEvent;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import com.sandbox.myfirstapp.app.models.Repo;
import com.sandbox.myfirstapp.app.models.SpaceTokenizer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import android.support.v7.widget.Toolbar;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

public class MyActivity extends BaseActivity {

    public static final String EXTRA_MESSAGE = "com.sandbox.myfirstapp.MESSAGE";
    public static final String EXTRA_VIDEO_URL = "com.sandbox.myfirstapp.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.sandbox.myfirstapp.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.sandbox.myfirstapp.WORD_LIST";
    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int SETTINGS_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int CHOOSE_CHARACTER_DRAWER_ITEM_IDENTIFIER = 4;

    private Context mContext;

    private MultiAutoCompleteTextView editText;
    private TextView debugView;
    private VideoView videoView;
    private String packageDir;
    private ProgressBar progressBar;

    private NavigationView navigationView;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

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
        debugView = (TextView) findViewById(R.id.display_debug);
        videoView = (VideoView) findViewById(R.id.video_view);

        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);

        initVideoPlayer();
        initChatText();
        initNavigationViewDrawer();
    }

    private void initVideoPlayer() {
        videoView.setMediaController(new MediaController(this));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.setBackgroundColor(Color.TRANSPARENT);
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
        String[] words = new String[] {
                "the", "this", "time", "tree", "tell", "tod", "take","tall", "tam", "taker", "taken",
                "tad", "tar", "tame", "tamer", "tap", "tape", "tale","tail", "tarzan", "tan",
                "hello", "world", "i", "am", "funny", "fun", "food","in", "what", "are", "you"
        };
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, words);
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
                                intent = new Intent(mContext, UserRepoListActivity.class);
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
        if (item.getItemId() == R.id.action_save) {
            Intent intent = new Intent(mContext, SaveActivity.class);
            intent.putExtra(EXTRA_VIDEO_URL, this.videoUrl);
            intent.putExtra(EXTRA_VIDEO_PATH, this.videoPath);
            intent.putExtra(EXTRA_WORD_LIST, this.wordList);
            startActivity(intent);
            return true;
        }
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

    public void sendMessage(View view) throws IOException {
        debugView.setText("");

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            progressBar.setVisibility(View.VISIBLE);

            String message = editText.getText().toString();
            MadchatClient.getQuery(message);
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

    public void playLocalVideo(View view) {
        //videoView.setVideoURI(Uri.parse("http://192.168.1.77:3000/repos/im_gonna_make_him_an_offers_he_cant_reject_1455179387.mp4"));
        videoView.setVideoPath(packageDir + "/mydownloadmovie.mp4");
        // videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kevin_hart_booty));
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
        this.wordList = event.wordList;
        this.videoUrl = event.videoUrl;
    }

    @Subscribe
    public void onEvent(VideoDownloadEvent event) {
        progressBar.setVisibility(View.GONE);

        if (event.error != null) {
            debugView.setText(event.error);
        } else {
            this.videoPath = event.videoPath;
            videoView.setVideoPath(this.videoPath);
            videoView.requestFocus();
            videoView.start();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }


}
