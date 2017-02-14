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
import com.roplabs.bard.util.*;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class ShareEditorActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private Context mContext;
    private GridView shareListView;
    private ProgressDialog progressDialog;
    private Repo repo;
    private String wordTagListString;
    private String sceneName;
    private String sceneToken;
    private boolean isPerformingLinkGeneration;
    private boolean isPerformingTextSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Share Editor onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_editor);

        shareListView = (GridView) findViewById(R.id.social_share_list);


        Intent intent = getIntent();
        sceneToken = intent.getStringExtra("sceneToken");
        sceneName = intent.getStringExtra("sceneName");
        wordTagListString = intent.getStringExtra("wordTags");

        String repoToken = intent.getStringExtra("repoToken");
        if (repoToken != null) {
            repo = Repo.forToken(repoToken);
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
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.facebook.orca");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Facebook Messenger", Toast.LENGTH_LONG).show();
        }
    }

    private void startWhatsappShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Whatsapp", Toast.LENGTH_LONG).show();
        }
    }

    private void startTelegramShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("org.telegram.messenger");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Telegram", Toast.LENGTH_LONG).show();
        }
    }

    private void startTwitterShare() {
        Intent intent = getRepoShareIntent();
        intent.setClassName("com.twitter.android", "com.twitter.android.composer.ComposerActivity");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Twitter", Toast.LENGTH_LONG).show();
        }
    }

    private void startKikShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("kik.android");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Kik", Toast.LENGTH_LONG).show();
        }
    }

    private void startEmailShare() {
        Intent intent = getRepoShareIntent();
        intent.putExtra(Intent.EXTRA_SUBJECT, "I made a Bard");
        intent.putExtra(Intent.EXTRA_TEXT, "I made this video using https://bard.co");

        try {
            intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            intent = getRepoShareIntent();
            intent.putExtra(Intent.EXTRA_SUBJECT, "I made a Bard");
            intent.putExtra(Intent.EXTRA_TEXT, "I made this video using https://bard.co");
            startActivity(Intent.createChooser(intent, "Send email"));
        }
    }

    private void startLinkShare() {
        final Context self = this;
        if (repo != null && (repo.getUrl() != null)) {
            // already published
            copyRepoLinkToClipboard(repo.getUrl());
            return;
        } else if (repo != null) {
            // repo exists but not yet published
            Helper.publishRepo(repo, this, new Helper.OnRepoPublished() {
                @Override
                public void onPublished(Repo publishedRepo) {
                    copyRepoLinkToClipboard(publishedRepo.getUrl());
                }
            });
        } else {
            Helper.saveLocalRepo(null, null, wordTagListString, sceneToken, sceneName, new Helper.OnRepoSaved() {
                @Override
                public void onSaved(Repo createdRepo) {
                    repo = createdRepo;
                    Helper.publishRepo(repo, self, new Helper.OnRepoPublished() {
                        @Override
                        public void onPublished(Repo publishedRepo) {
                            copyRepoLinkToClipboard(publishedRepo.getUrl());
                        }
                    });
                }
            });
        }
    }

    private void copyRepoLinkToClipboard(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(url, url);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ClientApp.getContext(), url + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void startTextShare() {
        final Context self = this;
        if (repo != null && (repo.getUrl() != null)) {
            // already published
            sendText(repo.getUrl());
            return;
        } else if (repo != null) {
            // repo exists but not yet published
            Helper.publishRepo(repo, this, new Helper.OnRepoPublished() {
                @Override
                public void onPublished(Repo publishedRepo) {
                    sendText(publishedRepo.getUrl());
                }
            });
        } else {
            Helper.saveLocalRepo(null, null, wordTagListString, sceneToken, sceneName, new Helper.OnRepoSaved() {
                @Override
                public void onSaved(Repo createdRepo) {
                    repo = createdRepo;
                    Helper.publishRepo(repo, self, new Helper.OnRepoPublished() {
                        @Override
                        public void onPublished(Repo publishedRepo) {
                            sendText(publishedRepo.getUrl());
                        }
                    });
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

        }
        else //For early versions, do what worked for you before.
        {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", text);
            activity.startActivity(sendIntent);
        }
    }

    private void startMoreShare() {
        startActivity(Intent.createChooser(getRepoShareIntent(), "Share"));
    }

    public Intent getRepoShareIntent() {
        Uri videoUri;

        if (this.repo == null) {
            File file = new File(Storage.getMergedOutputFilePath());
            videoUri = FileProvider.getUriForFile(ClientApp.getContext(), getApplicationContext().getPackageName() + ".provider", file);
        } else {
            File file = new File(this.repo.getFilePath());
            videoUri = FileProvider.getUriForFile(ClientApp.getContext(), getApplicationContext().getPackageName() + ".provider", file);
        }
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
