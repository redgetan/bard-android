package com.roplabs.bard.ui.activity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ShareListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.util.*;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.*;

public class ShareEditorActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private Context mContext;
    private GridView shareListView;
    private ProgressDialog progressDialog;
    private Repo repo;
    private Scene scene;
    private Character character;
    private String wordTagListString;
    private String sceneToken;
    private String characterToken;
    private String repoToken;
    private String shareType;
    private boolean isPerformingLinkGeneration;
    private boolean isPerformingTextSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Share Editor onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_editor);

        shareListView = (GridView) findViewById(R.id.social_share_list);


        Intent intent = getIntent();
        shareType = intent.getStringExtra("shareType");
        if (shareType == null) shareType = "";

        sceneToken = intent.getStringExtra("sceneToken");
        if (sceneToken != null) {
            scene = Scene.forToken(sceneToken);
        }

        wordTagListString = intent.getStringExtra("wordTags");

        repoToken = intent.getStringExtra("repoToken");
        if (repoToken != null) {
            repo = Repo.forToken(repoToken);
        }

        characterToken = intent.getStringExtra("characterToken");
        if (characterToken != null) {
            character = Character.forToken(characterToken);
        }

        initShare();
    }

    private void initShare() {
        // { name: "messenger", icon: "" }


//        PackageManager packageManager = getPackageManager();
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, "");
//        shareIntent.setType("video/mp4");
//
//        List<ResolveInfo> items = packageManager.queryIntentActivities(shareIntent, 0);

//        apps = sortAppSharing(apps);

        String apps[] = new String[] { "messenger", "whatsapp", "kik",
                                       "telegram", "twitter", "copy link",
                                       "email",     "text",   "more" } ;

        ShareListAdapter shareListAdapter = new ShareListAdapter(this, apps);
        shareListView.setAdapter(shareListAdapter);
        shareListView.setOnItemClickListener(this);
    }



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String app = (String) view.getTag();

        if (app.equals("messenger")) {
            startMessengerShare();
        } else if (app.equals("whatsapp")) {
            startWhatsappShare();
        } else if (app.equals("kik")) {
            startKikShare();
        } else if (app.equals("telegram")) {
            startTelegramShare();
        } else if (app.equals("twitter")) {
            startTwitterShare();
        } else if (app.equals("email")) {
            startEmailShare();
        } else if (app.equals("copy link")) {
            startLinkShare();
        } else if (app.equals("text")) {
            startTextShare();
        } else if (app.equals("more")) {
            startMoreShare();
        }

    }

    private void startMessengerShare() {
        Intent intent = getShareIntent();
        intent.setPackage("com.facebook.orca");

        try {
            startActivity(intent);
            trackSharing("messenger");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Facebook Messenger", Toast.LENGTH_LONG).show();
        }
    }

    private void startWhatsappShare() {
        Intent intent = getShareIntent();
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
            trackSharing("whatsapp");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Whatsapp", Toast.LENGTH_LONG).show();
        }
    }

    private void startTelegramShare() {
        Intent intent = getShareIntent();
        intent.setPackage("org.telegram.messenger");

        try {
            startActivity(intent);
            trackSharing("telegram");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Telegram", Toast.LENGTH_LONG).show();
        }
    }

    private void startTwitterShare() {
        Intent intent = getShareIntent();
        intent.setClassName("com.twitter.android", "com.twitter.android.composer.ComposerActivity");

        try {
            startActivity(intent);
            trackSharing("twitter");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Twitter", Toast.LENGTH_LONG).show();
        }
    }

    private void startKikShare() {
        Intent intent = getShareIntent();
        intent.setPackage("kik.android");

        try {
            startActivity(intent);
            trackSharing("kik");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Kik", Toast.LENGTH_LONG).show();
        }
    }

    private Intent getEmailShareIntent() {
        Intent intent = getShareIntent();
        if (shareType.equals("repo")) {
            intent.putExtra(Intent.EXTRA_SUBJECT, "I made a Bard");
            intent.putExtra(Intent.EXTRA_TEXT, "I made this video using https://bard.co");
        } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, "Let's Bard this Video");
            String shareUrl;
            if (character != null) {
                shareUrl = getPackUrl(characterToken);
                intent.putExtra(Intent.EXTRA_TEXT, "You can make this video say what you type. Try it at android/ios/web using the link " + shareUrl);
            } else if (scene != null) {
                shareUrl = getSceneUrl(sceneToken);
                intent.putExtra(Intent.EXTRA_TEXT, "You can make this video say what you type. Try it at android/ios/web using the link " + shareUrl);
            }
        }

        return intent;
    }


    private void startEmailShare() {
        Intent intent = getEmailShareIntent();

        try {
            intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
            startActivity(intent);
            trackSharing("email");
        }
        catch (android.content.ActivityNotFoundException ex) {
            intent = getEmailShareIntent();
            startActivity(Intent.createChooser(intent, "Send email"));
        } catch (SecurityException ex) {
            intent = getEmailShareIntent();
            startActivity(Intent.createChooser(intent, "Send email"));
        }

    }

    private void trackSharing(String medium) {
        Bundle params = new Bundle();

        params.putString("medium", medium);
        params.putString("sceneToken", sceneToken);

        if (scene != null) {
            params.putString("sceneName", scene.getName());
        }

        if (character != null) {
            params.putString("packName", character.getName());
            params.putString("packToken", character.getToken());
        }

        if (repo != null) {
            params.putString("repoToken", repoToken);
            params.putString("wordTags", wordTagListString);
        } else {
            params.putString("shareType", "scene");
        }

        Analytics.track(this, "shareSocialAttempt", params);
    }


    private void startLinkShare() {
        if (shareType.equals("repo")) {
            startRepoLinkShare();
        } else if (character != null) {
            startPackLinkShare();
        } else {
            startSceneLinkShare();
        }
    }

    private void startSceneLinkShare() {
        copyRepoLinkToClipboard(getSceneUrl(sceneToken));
    }

    private void startPackLinkShare() {
        copyRepoLinkToClipboard(getPackUrl(characterToken));
    }

    private void startRepoLinkShare() {
        final Context self = this;
        if (repo.getUrl() != null) {
            // already published
            copyRepoLinkToClipboard(repo.getUrl());
            return;
        } else {
            Helper.saveRemoteRepo(repo, repo.getUUID(), null, new Helper.OnRepoPublished() {
                @Override
                public void onPublished(HashMap<String, String> result) {
                    copyRepoLinkToClipboard(result.get("url"));
                }
            });
        }
    }

    private void copyRepoLinkToClipboard(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(url, url);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ClientApp.getContext(), url + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
        trackSharing("copylink");
    }

    private void startTextShare() {
        if (shareType.equals("repo")) {
            startRepoTextShare();
        } else if (character != null) {
            startPackTextShare();
        } else {
            startSceneTextShare();
        }
    }

    private void startSceneTextShare() {
        sendText(getSceneUrl(sceneToken));
    }

    private void startPackTextShare() {
        sendText(getPackUrl(characterToken));
    }

    private String getSceneUrl(String sceneToken) {
        return Configuration.bardAPIBaseURL() + "/scenes/" + sceneToken + "/editor";
    }

    private String getPackUrl(String characterToken) {
        return Configuration.bardAPIBaseURL() + "/packs/" + characterToken;
    }

    private void startRepoTextShare() {
        final Context self = this;
        if (repo.getUrl() != null) {
            // already published
            sendText(repo.getUrl());
            return;
        } else {
            // repo exists but not yet published
            Helper.saveRemoteRepo(repo, repo.getUUID(), null, new Helper.OnRepoPublished() {
                @Override
                public void onPublished(HashMap<String, String> result) {
                    sendText(result.get("url"));
                }
            });
        }
    }

    private void sendText(String text) {
        Context activity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) //At least KitKat
        {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity); //Need to change the build to API 19

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);

            if (defaultSmsPackageName != null)//Can be null in case that there is no default, then the user would be able to choose any app that support this intent.
            {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            activity.startActivity(sendIntent);
            trackSharing("text");
        }
        else //For early versions, do what worked for you before.
        {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", text);
            activity.startActivity(sendIntent);
            trackSharing("text");
        }
    }

    private void startMoreShare() {
        startActivity(Intent.createChooser(getShareIntent(), "Share"));
    }

    public Intent getShareIntent() {
        if (shareType.equals("repo")) {
            // share repo
            return getRepoShareIntent();
        } else if (sceneToken != null) {
            // share scene
            return getSceneShareIntent();
        } else if (character != null) {
            // share pack
            return getPackShareIntent();
        } else {
            return new Intent();
        }
    }

    public Intent getSceneShareIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String text = getSceneUrl(sceneToken);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        return sendIntent;
    }

    public Intent getPackShareIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String text = getPackUrl(characterToken);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        return sendIntent;
    }

    public Intent getRepoShareIntent() {
        Uri videoUri;

        File file = new File(this.repo.getFilePath());
        videoUri = FileProvider.getUriForFile(ClientApp.getContext(), getApplicationContext().getPackageName() + ".provider", file);
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }


    public void closeShare(View view) {
        if (this.repo != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("repoToken", this.repo.getToken());
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_OK);
        }
        finish();
    }
}
