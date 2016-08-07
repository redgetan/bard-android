package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import com.crashlytics.android.Crashlytics;
import com.roplabs.bard.R;
import com.roplabs.bard.db.DBMigration;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.FileManager;
import com.roplabs.bard.util.Helper;
import io.realm.Realm;
import io.realm.RealmConfiguration;
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent;
        String authToken = Setting.getAuthenticationToken(this);

        if (authToken.length() > 0) {
            intent = new Intent(this, CharacterSelectActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
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

}
