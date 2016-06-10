package com.roplabs.bard.models;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.events.VideoDownloadEvent;
import com.roplabs.bard.util.Helper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.BufferedSink;
import okio.Okio;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class VideoDownloader {
    private static final OkHttpClient client = new OkHttpClient();

    public static void downloadVideo(String url) {

        new DownloadVideoTask().execute(url);
    }

    public static void fetchSegments(List<Segment> segments) {
        final CountDownLatch responseCountDownLatch = new CountDownLatch(segments.size());
        final List<String> segmentPathList = new ArrayList<String>();

        for (final Segment segment : segments) {
            Request request = new Request.Builder()
                    .url(segment.getSourceUrl())
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.d("Bard", "failure on fetchSegments ");
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    // http://stackoverflow.com/a/29012988/803865
                    String[] sourceUrlTokens = segment.getSourceUrl().split("/");
                    String fileName = sourceUrlTokens[sourceUrlTokens.length - 1];
                    File downloadedFile = new File(ClientApp.getContext().getCacheDir(), fileName);
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                    sink.writeAll(response.body().source());
                    sink.close();

                    segment.setFilePath(downloadedFile.getAbsolutePath());
                    responseCountDownLatch.countDown();
                }
            });
        }

        try {
            responseCountDownLatch.await();
            EventBus.getDefault().post(new VideoDownloadEvent(segments));
        } catch (InterruptedException e) {
            HashMap<String, String> result = new HashMap<String, String>();
            result.put("error","interruption error");
            EventBus.getDefault().post(new VideoDownloadEvent(result));
        }

    }


    private static class DownloadVideoTask extends AsyncTask<String, Integer, HashMap<String, String>> {
        @Override
        protected HashMap<String, String> doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            return downloadFile(urls[0], this);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(HashMap<String, String> result) {
            EventBus.getDefault().post(new VideoDownloadEvent(result));
        }

        public void doProgress(int value){
            publishProgress(value);
        }

        @Override
        protected void onProgressUpdate(Integer[] progress) {
//            debugView.setText("progress:" + progress[0] + " %");
        }

        // http://stackoverflow.com/questions/20235553/download-the-video-before-play-it-on-android-videoview
        HashMap<String, String> downloadFile(String sourceUrl, DownloadVideoTask downloadVideoTask) {
            HashMap<String, String> result = new HashMap<String, String>();

            try {
                URL url = new URL(sourceUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");

                //connect
                urlConnection.connect();

                //create a new file, to save the downloaded file

                String packageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                               .getAbsolutePath();

                 File file = Helper.getSafeOutputFile(packageDir + "/Bard/", getSourceFileName(url));
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

                result.put("videoPath", file.getPath());
                result.put("error", null);
                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
                result.put("videoPath", null);
                result.put("error", e.getMessage());
                return result;
            } catch (IOException e) {
                result.put("videoPath", null);
                result.put("error", e.getMessage());
                return result;
            }
        }

        private String getSourceFileName(URL url) {
            int slashIndex = url.getPath().lastIndexOf("/");
            return url.getPath().substring(slashIndex + 1);
        }


    }



}
