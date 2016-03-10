package com.sandbox.myfirstapp.app.models;

import android.os.AsyncTask;
import android.os.Environment;
import com.sandbox.myfirstapp.app.events.VideoDownloadEvent;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.StringTokenizer;

public class VideoDownloader {
    public static void downloadVideo(String url) {
        new DownloadVideoTask().execute(url);
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

                File file = new File(packageDir + "/MadChat/", getSourceFileName(url));
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

        protected FileOutputStream getSafeOutputStream(String directory,String filename) {
            String filepath;
            if(directory.lastIndexOf(File.separator) != directory.length() - 1){
                directory += File.separator;
            }
            File dir = new File(directory);
            dir.mkdirs();
            filepath = directory + filename;
            File file = new File(filepath);
            try{
                file.createNewFile();
                return new FileOutputStream(file.getCanonicalFile().toString());
            }catch (Exception e){
                e.printStackTrace();
                throw new Error("Can not get an valid output stream");
            }
        }

        private String getSourceFileName(URL url) {
            int slashIndex = url.getPath().lastIndexOf("/");
            return url.getPath().substring(slashIndex + 1);
        }


    }



}
