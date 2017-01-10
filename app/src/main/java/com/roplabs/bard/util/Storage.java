package com.roplabs.bard.util;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.jakewharton.disklrucache.DiskLruCache;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Segment;
import com.roplabs.bard.models.VideoDownloader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


public class Storage {
    private static final int NUM_OF_VALUES_IN_VIDEO_CACHE_KEY = 1;
    private static final int VIDEO_CACHE_SIZE = 20 * 1024 * 1024; // 20mb

    private static DiskLruCache videoCache;

    public static String getSharedMoviesDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .getAbsolutePath() + "/" +  ClientApp.getContext().getResources().getString(R.string.app_name) + "/";
    }

    public static String getMergedOutputFilePath() {
        return getSharedMoviesDir() + "last_merge.mp4";
    }

    public static String getLocalSavedFilePath() {
        return getSharedMoviesDir() + Helper.getTimestamp() + ".mp4";
    }


    // the filepath of cache entry is not publicly exposed by DiskLRUCache
    // but it can be easily determinted by the convention
    //   "directory/" + key + "." + i
    // based on
    // https://github.com/JakeWharton/DiskLruCache/blob/disklrucache-2.0.2/src/main/java/com/jakewharton/disklrucache/DiskLruCache.java#L923
    public static String getCachedVideoFilePath(String wordTagString) {
        String cacheKey = getCacheKey(wordTagString);
        int index = NUM_OF_VALUES_IN_VIDEO_CACHE_KEY - 1;
        return ClientApp.getContext().getExternalCacheDir().getAbsolutePath() + "/" + cacheKey + "." + index;
    }

    public static String getCacheKey(String wordTagString) {
        String key = TextUtils.join("-",wordTagString.split(":")).toLowerCase();
        key = key.replaceAll("[^a-z0-9_-]","_"); // only allow valid keys, replace invalid with _
        return key;
    }

    public static DiskLruCache getDiskCache() {
        if (videoCache == null) {
            videoCache = openDiskCache();
        }

        return videoCache;
    }

    public static DiskLruCache openDiskCache() {
        File cacheDir = ClientApp.getContext().getExternalCacheDir();
        int appVersion = 1; // we dont care about app version...maybe
        try {
            return DiskLruCache.open(cacheDir, appVersion, NUM_OF_VALUES_IN_VIDEO_CACHE_KEY, VIDEO_CACHE_SIZE);
        } catch (IOException e) {
            CrashReporter.logException(e);
            e.printStackTrace();
            return null;
        }
    }


    public interface OnCacheVideoListener  {
        // This can be any number of events to be sent to the activity
        public void onCacheVideoSuccess(String fileUrl);
        public void onCacheVideoFailure();
    }

    // reference code for writing binary data to DiskLRUCache
    // http://www.programcreek.com/java-api-examples/index.php?source_dir=petsworld-master/ActionBar/libary/libcore/io/ImageCache.java
    public static void cacheVideo(final String wordTagString, String sceneToken, final OnCacheVideoListener cacheVideoListener) {
        String cacheKey = Storage.getCacheKey(wordTagString);
        try {
            final DiskLruCache.Editor editor = getDiskCache().edit(cacheKey);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                VideoDownloader.downloadUrlToStream(Segment.sourceUrlFromWordTagString(wordTagString, sceneToken), outputStream, new VideoDownloader.OnDownloadListener() {
                    @Override
                    public void onDownloadSuccess() {
                        try {
                            editor.commit();
                            getDiskCache().flush();
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // code to interact with UI
                                    cacheVideoListener.onCacheVideoSuccess(Storage.getCachedVideoFilePath(wordTagString));
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // code to interact with UI
                                    cacheVideoListener.onCacheVideoFailure();
                                }
                            });
                        }
                    }

                    @Override
                    public void onDownloadFailure() {
                        try {
                            editor.abort();
                            getDiskCache().flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // code to interact with UI
                                cacheVideoListener.onCacheVideoFailure();
                            }
                        });
                    }
                });

            }

        } catch (IOException e) {
            e.printStackTrace();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // code to interact with UI
                    cacheVideoListener.onCacheVideoFailure();
                }
            });
        }
    }

}
