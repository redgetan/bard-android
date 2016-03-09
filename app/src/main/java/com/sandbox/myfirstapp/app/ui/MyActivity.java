package com.sandbox.myfirstapp.app.ui;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
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
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.api.MadchatClient;
import com.sandbox.myfirstapp.app.events.VideoDownloadEvent;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.*;

public class MyActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static final String EXTRA_MESSAGE = "com.sandbox.myfirstapp.MESSAGE";

    private Context mContext;

    private EditText editText;
    private TextView debugView;
    private VideoView videoView;
    private String packageDir;
    private ProgressBar progressBar;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle;

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

        initNavigationDrawer();
    }

    private void initNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerItems = getResources().getStringArray(R.array.drawer_items);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerItems));
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_setting) {
//            Intent intent = new Intent(mContext, SettingActivity.class);
//            startActivity(intent);
//            return true;
//        }
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
        progressBar.setVisibility(View.GONE);

//        HttpProxyCacheServer proxy = VideoCacheProxy.getProxy(this);
//        String proxyUrl = proxy.getProxyUrl(event.videoUrl);

        if (event.error != null) {
            debugView.setText(event.error);
        } else {
            videoView.setVideoPath(event.videoUrl);
            videoView.requestFocus();
            videoView.start();
        }

//        Method method = HttpProxyCacheServer.class.getDeclaredMethod("getClients", String.class);
//        method.setAccessible(true);
//        method.invoke(proxy, event.videoUrl);
    }

    @Subscribe
    public void onEvent(VideoDownloadEvent event) {
        progressBar.setVisibility(View.GONE);

        if (event.error != null) {
            debugView.setText(event.error);
        } else {
            videoView.setVideoPath(event.videoPath);
            videoView.requestFocus();
            videoView.start();
        }

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //selectItem(position);

        String title = ((TextView) view.findViewById(R.id.item_name)).getText().toString();
        if (title.equals("My Projects")) {
            Intent intent = new Intent(mContext, ProjectListActivity.class);
            startActivity(intent);

            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

}
