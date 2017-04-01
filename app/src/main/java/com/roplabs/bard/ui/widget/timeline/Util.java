package com.roplabs.bard.ui.widget.timeline;

import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.TimeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

public class Util {

    public static String timeStamp(long position, long duration) {
        StringBuilder posTime = new StringBuilder();
        TimeUtils.formatDuration(position, posTime);
        StringBuilder durationTime = new StringBuilder();
        TimeUtils.formatDuration(duration, durationTime);

        return posTime + " / " + durationTime.toString();
    }

    public static File[] loadMovieFolder() throws FileNotFoundException {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .listFiles(new FilenameFilter() {
                    @Override public boolean accept(File dir, String filename) {
                        return filename.endsWith(".mp4");
                    }
                });
    }

    public static String genVideoId(@NonNull Uri videoUri, int playbackOrder, Object... manifest) {
        return genVideoId(videoUri.toString(), playbackOrder, manifest);
    }

    public static String genVideoId(@NonNull String videoUri, int playbackOrder, Object... manifest) {
        StringBuilder builder = new StringBuilder();
        builder.append(videoUri).append(":").append(playbackOrder);
        if (manifest != null && manifest.length > 0) {
            for (Object o : manifest) {
                builder.append(":").append(o.toString());
            }
        }

        return builder.toString();
    }
}

