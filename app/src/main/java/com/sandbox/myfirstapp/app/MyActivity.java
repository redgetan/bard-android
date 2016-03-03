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
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import com.sandbox.myfirstapp.app.api.MadchatClient;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import com.sandbox.myfirstapp.app.util.CustomJSONSerializer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.*;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        packageDir = getExternalFilesDir(null).getAbsolutePath();
        debugView = (TextView) findViewById(R.id.display_debug);
        videoView = (VideoView) findViewById(R.id.video_view);
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
        if (networkInfo != null && networkInfo.isConnected()) {

            EditText editText = (EditText) findViewById(R.id.edit_message);
            String message = editText.getText().toString();
            MadchatClient.getQuery(message);
            // fetch data
            //String stringUrl = "http://www.indeed.ca";
//            String stringUrl = "http://192.168.1.77:3000/repos/Y32OEovQHi4FHp8.mp4";
//            new DownloadVideoTask().execute(stringUrl);
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

    private class DownloadVideoTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                downloadFile(urls[0], this);
                return "success";
            } catch (IOException e) {
                return "failure";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if (result.equals("success")) {
                debugView.setText("");
                videoView.setVideoPath(packageDir + "/mydownloadmovie.mp4");
                videoView.requestFocus();
                videoView.start();
            } else {
                debugView.setText("Unable to retrieve video");
            }
        }

        public void doProgress(int value){
            publishProgress(value);
        }

        @Override
        protected void onProgressUpdate(Integer[] progress) {
            debugView.setText("progress:" + progress[0] + " %");
        }
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
    public void onVideoQueryEvent(VideoQueryEvent event){
        videoView.setVideoURI(Uri.parse(event.videoUrl));
        videoView.requestFocus();
        videoView.start();
    }

    // http://stackoverflow.com/questions/20235553/download-the-video-before-play-it-on-android-videoview
    void downloadFile(String sourceUrl, DownloadVideoTask downloadVideoTask) throws IOException {

        try {
            URL url = new URL(sourceUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");

            //connect
            urlConnection.connect();

            //create a new file, to save the downloaded file
            File file = new File(packageDir, getSourceFileName(url));

            FileOutputStream fileOutput = new FileOutputStream(file);

            //Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            //this is the total size of the file which we are downloading
            int totalSize = urlConnection.getContentLength();


            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            int downloadedSize = 0;
            int progress;

            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                progress = (downloadedSize * 100 / totalSize);
                downloadVideoTask.doProgress(progress);
            }
            //close the output stream when complete //
            fileOutput.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
            debugView.setText("Unable to retrive video. malformed url");
        }
    }

    private String getSourceFileName(URL url) {
        int slashIndex = url.getPath().lastIndexOf("/");
        return url.getPath().substring(slashIndex + 1);
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("MARIO", "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}
