package com.roplabs.madchat.util;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.util.ArrayList;

public class CustomJSONSerializer {
    private Context mContext;
    private String mFilename;
    public CustomJSONSerializer(Context c, String f) {
        mContext = c;
        mFilename = f;
    }
    public void save(ArrayList<String> names)
            throws JSONException, IOException {
        // Build an array in JSON
        JSONArray array = new JSONArray();
        for (String name : names)
            array.put(name);

        // Write the file to disk
        File file = null;
        FileOutputStream stream = null;
        try {
            file = new File(mFilename);
            stream = new FileOutputStream(file);
            stream.write(array.toString().getBytes());
        } finally {
            if (stream != null)
                stream.close();
        }
    }
}
