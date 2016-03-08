package com.sandbox.myfirstapp.app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.danikula.videocache.HttpProxyCacheServer;
import com.sandbox.myfirstapp.app.api.MadchatClient;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import com.sandbox.myfirstapp.app.models.VideoCacheProxy;
import com.sandbox.myfirstapp.app.util.CustomJSONSerializer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

public class MyActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.sandbox.myfirstapp.MESSAGE";
    private TextView debugView;
    private VideoView videoView;
    private String packageDir;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        packageDir = getExternalFilesDir(null).getAbsolutePath();
        debugView = (TextView) findViewById(R.id.display_debug);
        videoView = (VideoView) findViewById(R.id.video_view);
        progressBar = (ProgressBar) findViewById(R.id.query_video_progress_bar);
        videoView.setMediaController(new MediaController(this));

        String dataDir = Environment.getDataDirectory().getAbsolutePath();
        File picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        String filesDir = getFilesDir().getAbsolutePath();
//        Log.d("MARIO", dataDir);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendMessage(View view) throws IOException {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        progressBar.setVisibility(View.VISIBLE);

//        new android.os.Handler().postDelayed(
//            new Runnable() {
//                public void run() {
//                    progressBar.setVisibility(View.GONE);
//                }
//            },
//        3000);

        if (networkInfo != null && networkInfo.isConnected()) {

            EditText editText = (EditText) findViewById(R.id.edit_message);
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
    public void onEvent(VideoQueryEvent event) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        progressBar.setVisibility(View.GONE);

//        HttpProxyCacheServer proxy = VideoCacheProxy.getProxy(this);
//        String proxyUrl = proxy.getProxyUrl(event.videoUrl);
        videoView.setVideoPath(event.videoUrl);
        videoView.requestFocus();
        videoView.start();

//        Method method = HttpProxyCacheServer.class.getDeclaredMethod("getClients", String.class);
//        method.setAccessible(true);
//        method.invoke(proxy, event.videoUrl);
    }

}
