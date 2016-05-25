package com.roplabs.bard.ui;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import com.crashlytics.android.Crashlytics;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Index;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.FileManager;
import com.roplabs.bard.util.Helper;
import io.realm.RealmResults;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends BaseActivity {
    private String applicationDir;
    private String ffmpegPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + "ffmpeg";
        initFFmpeg();
        initWordIndex();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent;
        String authToken = Setting.getAuthenticationToken(this);

        if (authToken.length() > 0) {
            intent = new Intent(this, IndexActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
    }

    public void initFFmpeg() {
        String binary = "ffmpeg";

        if (!(new File(ffmpegPath)).exists()) {

            // copy ffmpeg binary from assets folder to /data/data/com.roplabs.*
            try {
                InputStream inputStream = getAssets().open(binary);
                File file = Helper.getSafeOutputFile(applicationDir, binary);
                Helper.writeToFile(inputStream, file);
            } catch (IOException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }

            Helper.runCmd(new String[] { "/system/bin/chmod", "744", ffmpegPath});
        }
    }

    public void populateWordIndex(String indexFileName) throws IOException, JSONException {
        AssetManager assetManager = getAssets();
        InputStream input = assetManager.open(indexFileName);

        JSONObject obj = new JSONObject(FileManager.readInputStream(input));

        Index.create(obj.getString("token"),
                obj.getString("name"),
                obj.getString("description"),
                obj.getString("wordList"));
    }

    public void initWordIndex() {
        RealmResults<Index> indexResults = Index.findAll();
        if (indexResults.size() == 0) {
            try {
                populateWordIndex("smosh_index.json");
                populateWordIndex("donald_trump_index.json");
                populateWordIndex("kevin_hart_index.json");
                populateWordIndex("emma_watson_index.json");

                setDefaultIndex();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDefaultIndex() {
        Setting.setCurrentIndexToken(this,Index.findFirst().getToken());
    }


}
