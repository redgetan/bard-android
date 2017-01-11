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
    private Button saveRepoBtn;
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
        saveRepoBtn = (Button) findViewById(R.id.save_repo_btn);


        Intent intent = getIntent();
        sceneToken = intent.getStringExtra("sceneToken");
        sceneName = intent.getStringExtra("sceneName");
        wordTagListString = intent.getStringExtra("wordTags");

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
                                       "tumblr",     "text",   "more" } ;

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
        } else if (app.equals("tumblr")) {
            startTumblrShare();
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

    private void startTumblrShare() {
        Intent intent = getRepoShareIntent();
        intent.setPackage("com.tumblr");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,"Please Install Tumblr", Toast.LENGTH_LONG).show();
        }
    }

    private void startLinkShare() {
        if (repo != null) {
            copyRepoLinkToClipboard();
            return;
        }
        isPerformingLinkGeneration = true;
        saveRepo(null);
    }

    private void copyRepoLinkToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(this.repo.getUrl(), this.repo.getUrl());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ClientApp.getContext(), this.repo.getUrl() + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void startTextShare() {
        if (repo != null) {
            sendText(repo.getUrl());
            return;
        }
        isPerformingTextSend = true;
        saveRepo(null);
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

    private String getRepositoryS3Key(String uuid) {
        return "repositories/" + Setting.getUsername(this) + "/" + uuid + ".mp4";
    }

    public void saveRepo(View view) {
        if (this.repo != null) {
            // already saved (i.e. when generating online link)
            setResult(RESULT_OK);
            finish();
            return;
        }

        saveRepoBtn.setEnabled(false);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Saving...");
        progressDialog.show();

        final String wordList = wordTagListString;

        // upload to S3

        final String uuid = UUID.randomUUID().toString();
        AmazonS3 s3 = new AmazonS3Client(AmazonCognito.credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        TransferObserver observer = transferUtility.upload(
                Configuration.s3UserBucket(),
                getRepositoryS3Key(uuid),
                new File(Storage.getMergedOutputFilePath())
        );

        observer.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                // do something
                if (state == TransferState.COMPLETED) {
                    saveRemoteRepo(uuid, wordList);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                //Display percentage transfered to user
            }

            @Override
            public void onError(int id, Exception ex) {
                // do something
                displayError("Unable to upload to server", ex);
            }

        });

    }

    private void saveRemoteRepo(String uuid, final String wordList) {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("uuid", uuid);
        body.put("word_list", wordList);
        body.put("scene_token", sceneToken);
//        body.put("character_token", this.characterToken);
        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postRepo(body);

        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (!response.isSuccess()) {
                    displayError("Unable to sync to remote server", new Throwable("Failed to save repo to bard server"));
                } else {
                    HashMap<String, String> result = response.body();
                    saveLocalRepo(result.get("token"), result.get("url"), wordList);
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                displayError("Unable to sync to remote server", t);
            }
        });
    }

    private void displayError(String message, Throwable t) {
        saveRepoBtn.setEnabled(true);
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(t);
    }

    private void displayError(String message) {
        saveRepoBtn.setEnabled(true);
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(new Throwable(message));
    }

    private void saveLocalRepo(String token, String url, String wordList) {
        String filePath = Storage.getLocalSavedFilePath();

        if (Helper.copyFile(Storage.getMergedOutputFilePath(),filePath)) {
            this.repo = Repo.create(token, url, "", sceneToken, filePath, wordList, Calendar.getInstance().getTime());

            JSONObject properties = new JSONObject();
            try {
                properties.put("wordTags", wordTagListString);
                properties.put("sceneToken", sceneToken);
                properties.put("scene", sceneName);
//                properties.put("character", character.getName());
            } catch (JSONException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
            Analytics.track(this, "saveRepo", properties);

            progressDialog.dismiss();

            if (isPerformingLinkGeneration) {
                isPerformingLinkGeneration = false;
                copyRepoLinkToClipboard();
            } else if (isPerformingTextSend) {
                isPerformingTextSend = false;
                sendText(repo.getUrl());
            } else {
                saveRepoBtn.setText("Saved");

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_OK);
                        finish();
                    }
                }, 500);
            }

            saveRepoBtn.setEnabled(true);

        } else {
            displayError("Unable to save to phone");
        }
    }

    public Intent getRepoShareIntent() {
        Uri videoUri;

        if (this.repo == null) {
            videoUri = Uri.fromFile(new File(Storage.getMergedOutputFilePath()));
        } else {
            videoUri = Uri.fromFile(new File(this.repo.getFilePath()));
        }
        // Create share intent as described above
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.setType("video/mp4");
        return shareIntent;
    }


    public void closeShare(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
