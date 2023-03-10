package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import com.crashlytics.android.Crashlytics;
import com.roplabs.bard.R;
import com.roplabs.bard.db.DBMigration;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.Analytics;
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
    private String sceneTokenEditorDeepLink;
    private String packTokenEditorDeepLink;
    private String channelTokenDeepLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applicationDir = getApplicationInfo().dataDir;
        ffmpegPath = applicationDir + "/" + Helper.ffmpegBinaryName();
        initFFmpeg();

    }

    private void handleDeepLink(Intent intent) {
        // check if activity launched via deeplink url
        Uri uri = intent.getData();
        if (uri != null && uri.toString().contains("editor")) {
            // extract sceneToken from https://bard.co/scenes/:token/editor
            String result = uri.toString().split("/editor")[0];
            String[] components = result.split("/");
            sceneTokenEditorDeepLink = components[components.length - 1];
        } else if (uri != null && uri.toString().contains("/packs/")) {
            // extract sceneToken from https://bard.co/packs/:token
            String result = uri.toString().split("/packs/")[1];
            packTokenEditorDeepLink = result;
        } else if (uri != null && uri.toString().contains("/channels/")) {
            // extract sceneToken from https://bard.co/channels/:token
            String result = uri.toString().split("/channels/")[1];
            if (result.charAt(result.length() - 1) == '/') {
                // opened via intent: uri scheme -> remove trailing slash
                result = result.substring(0, result.length() - 1);
            }
            channelTokenDeepLink = result;
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleDeepLink(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent;
        String authToken = Setting.getAuthenticationToken(this);

        if (authToken.length() > 0) {
            Analytics.identify(this);
        }

        intent = new Intent(this, SceneSelectActivity.class);
        intent.putExtra("mode", "default");
        intent.putExtra("sceneTokenEditorDeepLink", sceneTokenEditorDeepLink);
        intent.putExtra("packTokenEditorDeepLink", packTokenEditorDeepLink);
        intent.putExtra("channelTokenDeepLink", channelTokenDeepLink);

        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void initFFmpeg() {
        String binary = Helper.ffmpegBinaryName();

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

            Helper.runCmd(new String[]{"/system/bin/chmod", "744", ffmpegPath}, new Helper.ProcessListener() {
                @Override
                public void onProcessAvailable(Process process) {

                }
            });
        }
    }

}
