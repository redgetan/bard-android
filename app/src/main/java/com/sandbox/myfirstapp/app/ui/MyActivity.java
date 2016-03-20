package com.sandbox.myfirstapp.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
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
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import android.support.v7.widget.Toolbar;

import java.io.*;
import java.util.Calendar;
import java.util.Date;

public class MyActivity extends BaseActivity {

    public static final String EXTRA_MESSAGE = "com.sandbox.myfirstapp.MESSAGE";
    public static final String EXTRA_VIDEO_URL = "com.sandbox.myfirstapp.VIDEO_URL";
    public static final String EXTRA_VIDEO_PATH = "com.sandbox.myfirstapp.VIDEO_PATH";
    public static final String EXTRA_WORD_LIST = "com.sandbox.myfirstapp.WORD_LIST";

    private Context mContext;

    private EditText editText;
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

        editText = (EditText) findViewById(R.id.edit_message);
        packageDir = getExternalFilesDir(null).getAbsolutePath();
        debugView = (TextView) findViewById(R.id.display_debug);
        videoView = (VideoView) findViewById(R.id.video_view);
        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        videoView.setMediaController(new MediaController(this));

        initNavigationViewDrawer();
    }

    private void initNavigationViewDrawer() {
// Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.profile_header)
                .addProfiles(
                        new ProfileDrawerItem().withName("Mike Penz").withEmail("mikepenz@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.create_string),
                        new PrimaryDrawerItem().withName(R.string.my_projects_string),
                        new PrimaryDrawerItem().withName(R.string.settings_string)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        return true;
                    }
                })
                .build();

    }


//    /* Called whenever we call invalidateOptionsMenu() */
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
////        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
//        return super.onPrepareOptionsMenu(menu);
//    }

//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        // Sync the toggle state after onRestoreInstanceState has occurred.
//        mDrawerToggle.syncState();
//    }
//
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        mDrawerToggle.onConfigurationChanged(newConfig);
//    }

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
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_save) {
//            Intent intent = new Intent(mContext, SaveActivity.class);
//            intent.putExtra(EXTRA_VIDEO_URL, this.videoUrl);
//            intent.putExtra(EXTRA_VIDEO_PATH, this.videoPath);
//            intent.putExtra(EXTRA_WORD_LIST, this.wordList);
//            startActivity(intent);
//            return true;
//        }
//        if (item.getItemId() == android.R.id.home) {
//            if (mDrawerLayout.isDrawerOpen(mDrawerLayout.getChildAt(1)))
//                mDrawerLayout.closeDrawers();
//            else {
//                mDrawerLayout.openDrawer(mDrawerLayout.getChildAt(1));
//            }
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

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
