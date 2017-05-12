package com.roplabs.bard.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.ui.activity.*;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.support.v7.widget.Toolbar;

import java.io.*;
import java.lang.Process;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;


public class Helper {

    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int UPLOAD_VIDEO_DRAWER_ITEM_IDENTIFIER = 4;
    public static final int PROFILE_DRAWER_ITEM_IDENTIFIER = 5;
    public static final int TELL_FRIEND_DRAWER_ITEM_IDENTIFIER = 6;
    public static final int MY_PACKS_DRAWER_ITEM_IDENTIFIER = 7;
    public static final int MY_CHANNELS_DRAWER_ITEM_IDENTIFIER = 8;
    public static final int MY_LIKES_DRAWER_ITEM_IDENTIFIER = 9;

    public static final int REQUEST_WRITE_STORAGE = 1;
    public static final int LOGIN_REQUEST_CODE = 2;
    public static final int PROFILE_REQUEST_CODE = 3;
    public static final int SHARE_REPO_REQUEST_CODE = 4;
    public static final int SEARCH_REQUEST_CODE = 5;
    public static final int SHARE_SCENE_REQUEST_CODE = 6;
    public static final int SHARE_PACK_REQUEST_CODE = 7;
    public static final int CHANNEL_REQUEST_CODE = 8;
    public static final int BARD_EDITOR_REQUEST_CODE = 9;
    public static final int EDITOR_PREVIEW_REQUEST_CODE = 10;
    public static final int VIDEO_PLAYER_REQUEST_CODE = 11;
    public static final int CHOOSE_FILE_UPLOAD_REQUEST_CODE = 12;
    public static final int SCENE_SELECT_REQUEST_CODE = 13;
    public static final int SEARCH_USERNAME_REQUEST_CODE = 14;
    public static final int INVITE_CONTACT_REQUEST_CODE = 15;
    public static final int NEW_MESSAGE_REQUEST_CODE = 16;
    public static final int FORGOT_PASSWORD_REQUEST_CODE = 17;
    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 18;

    public static final String POPULAR_SCENE_TYPE = "top";
    public static final String FAVORITES_SCENE_TYPE = "favorites";
    public static final String UPLOADS_SCENE_TYPE = "uploads";
    public static final String ONLINE_LIBRARY = "latest";
    public static final String POLITICS_SCENE_TYPE = "politics";
    public static final String VIDEO_GAMES_SCENE_TYPE = "video_games";
    public static final String CARTOON_SCENE_TYPE = "cartoon";
    public static final String TELEVISION_SCENE_TYPE = "television";
    public static final String MOVIES_SCENE_TYPE = "movies";
    public static final String YOUTUBER_SCENE_TYPE = "youtuber";
    public static final String INTERVIEW_SCENE_TYPE = "interview";
    public static final String COMEDY_SCENE_TYPE = "comedy";
    public static final String PUPPETS_SCENE_TYPE = "puppets";
    public static final String OTHERS_SCENE_TYPE = "other";

    public static final String CHANNEL_FEED = "channel_feed";
    public static final String CHANNEL_VIDEOS = "channel_videos";
    public static final String PACKS = "packs";
    public static final String BARD_CREATE = "bard_create";
    public static final String PROFILE = "profile" ;
    public static final String SCENES = "scenes" ;
    public static final String TYPE_TEXT = "type_text" ;


    private static ProgressDialog progressDialog;

    public static String parseError(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                String json = response.errorBody().string();
                JSONObject jsonObj = new JSONObject(json);
                return jsonObj.getString("error");
            } catch (IOException e) {
                return "";
            } catch (JSONException e) {
                return "";
            }
        } else {
           return "";
        }
    }

    public static String getAppVersion() {
        String result = "";

        try {
            PackageInfo pInfo = ClientApp.getContext().getPackageManager().getPackageInfo(ClientApp.getContext().getPackageName(), 0);
            result = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return result;
    }

    public static Boolean isConnectedToInternet() {
        ConnectivityManager connMgr = (ConnectivityManager) ClientApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    public static String normalizeWord(CharSequence word) {
        return word.toString().toLowerCase().replaceAll("[\"\'.?!]","");
    }

    public interface ProcessListener {
        public void onProcessAvailable(Process process);
    }


    // http://gimite.net/en/index.php?Run%20native%20executable%20in%20Android%20App
    public static String runCmd(String[] cmd, ProcessListener processListener) {
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec(cmd);
            processListener.onProcessAvailable(process);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTimestamp() {
        Long tsLong = System.currentTimeMillis()/1000;
        return tsLong.toString();
    }

    public static void writeToFile(InputStream inputStream, File file) throws IOException {
        OutputStream output = new FileOutputStream(file);

        int read;
        byte[] buffer = new byte[4096];

        while ((read = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, read);
        }

        inputStream.close();
    }

    public static String ffmpegBinaryName() {
        String arch = System.getProperty("os.arch");
        if ((arch.contains("x86")) || (arch.contains("i686"))) {
            return "ffmpeg_x86";
        } else {
            return "ffmpeg";
        }
    }

    private static boolean hasNeon() {
        String archInfo = getArchInfo();
        return archInfo.toLowerCase(Locale.ENGLISH).contains("neon");
    }

    private static String getArchInfo() {
        StringBuffer sb = new StringBuffer();

        if (new File("/proc/cpuinfo").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine + "\n");
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }


    public static File getSafeOutputFile(String directory, String filename) {
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
            return file.getCanonicalFile();
        }catch (IOException e){
            e.printStackTrace();
            throw new Error("Can not get an valid output file");
        }
    }

    public static boolean copyFile(String inputPath, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            in = new FileInputStream(inputPath);
            out = new FileOutputStream(outputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;

            return true;
        }  catch (FileNotFoundException fnfe1) {
            BardLogger.trace(fnfe1.getMessage());
            return false;
        }
        catch (Exception e) {
            BardLogger.trace(e.getMessage());
            return false;
        }

    }

    public interface KeyboardVisibilityListener {
        void onKeyboardVisibilityChanged(boolean keyboardVisible, int keyboardHeight);
    }

    public static void setKeyboardVisibilityListener(Activity activity, final View parentLayout) {
        final KeyboardVisibilityListener keyboardVisibilityListener = (KeyboardVisibilityListener) activity;
        parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int mPreviousKeyboardHeight = 0;

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();

                parentLayout.getWindowVisibleDisplayFrame(r);

                int screenHeight = parentLayout.getRootView().getHeight();
                int keyboardHeight = screenHeight - (r.bottom);
                if (keyboardHeight > mPreviousKeyboardHeight) {
                    // Height decreased: keyboard was shown (difference > 100 - assume keyboard)
//                    BardLogger.log("height show: " + keyboardHeight + " - " + mPreviousKeyboardHeight);
                    keyboardVisibilityListener.onKeyboardVisibilityChanged(true, keyboardHeight);
                } else if (keyboardHeight < mPreviousKeyboardHeight) {
//                    BardLogger.log("height hide: " + keyboardHeight + " - " + mPreviousKeyboardHeight);
                    // Height increased: keyboard was hidden
                    keyboardVisibilityListener.onKeyboardVisibilityChanged(false, keyboardHeight);
                } else {
                    // No change
                }
                mPreviousKeyboardHeight = keyboardHeight;
            }
        });
    }

    public static boolean isPathUrl(String path) {
        return path.startsWith("http");
    }

    public static void initNavigationViewDrawer(final AppCompatActivity context, Toolbar toolbar) {
        String username = Setting.getUsername(context);
        ProfileDrawerItem profileDrawerItem;
        final List<Repo> repos = Repo.forUsername(Setting.getUsername(context));
        final List<Like> userLikes = Like.forUsername(Setting.getUsername(context));
        String libraryCount = String.valueOf(repos.size());
        String likeCount = String.valueOf(userLikes.size());

        final List<Channel> channels = Channel.forUsername(Setting.getUsername(context));
        String channelCount = String.valueOf(channels.size());

        if (username.equals("anonymous")) {
            profileDrawerItem = new ProfileDrawerItem().withName("Click Avatar to Login"); // .withIcon(getResources().getDrawable(R.drawable.profile))
        } else {
            profileDrawerItem = new ProfileDrawerItem().withName(username); // .withIcon(getResources().getDrawable(R.drawable.profile))
        }

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(context)
                .withHeaderBackground(R.drawable.profile_header)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(profileDrawerItem)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (!Setting.isLogined(context)) {
                            Intent intent = new Intent(context, LoginActivity.class);
                            context.startActivityForResult(intent, LOGIN_REQUEST_CODE);
                        }
                        return false;
                    }
                })
                .withHeightDp(150)
                .build();

        int textColor = ContextCompat.getColor(ClientApp.getContext(), R.color.black);
//        int badgeColor = ContextCompat.getColor(ClientApp.getContext(), R.color.white);

        new DrawerBuilder()
                .withActivity(context)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Upload a Video").withTextColor(textColor).withIdentifier(UPLOAD_VIDEO_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_videocam_black_24dp),
//                        new PrimaryDrawerItem().withName(R.string.my_channels).withTextColor(textColor).withIdentifier(MY_CHANNELS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_theaters_black_24dp).withBadge(channelCount).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.jumbo)),
                        new PrimaryDrawerItem().withName(R.string.bard_library).withTextColor(textColor).withIdentifier(MY_PROJECTS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_inbox_black_24dp).withBadge(libraryCount).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.jumbo)),
                        new PrimaryDrawerItem().withName(R.string.my_likes).withTextColor(textColor).withIdentifier(MY_LIKES_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_favorite_border_black_24dp).withBadge(likeCount).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.jumbo)),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.tell_friend).withTextColor(textColor).withIdentifier(TELL_FRIEND_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_person_add_black_24dp),
                        new PrimaryDrawerItem().withName(R.string.settings_string).withTextColor(textColor).withIdentifier(PROFILE_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_settings_black_24dp)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        Intent intent;

                        switch ((int) drawerItem.getIdentifier()) {
                            case MY_PROJECTS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(context.getApplicationContext(), RepoListActivity.class);
                                intent.putExtra("repoListType","created");
                                context.startActivity(intent);
                                break;
                            case MY_LIKES_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(context.getApplicationContext(), RepoListActivity.class);
                                intent.putExtra("repoListType","likes");
                                context.startActivity(intent);
                                break;
                            case MY_CHANNELS_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(context.getApplicationContext(), ChannelListActivity.class);
                                context.startActivity(intent);
                                break;
                            case UPLOAD_VIDEO_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(context.getApplicationContext(), UploadVideoActivity.class);
                                context.startActivity(intent);
                                break;
                            case TELL_FRIEND_DRAWER_ITEM_IDENTIFIER:
                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, you should check out https://bard.co");
                                shareIntent.setType("text/plain");
                                context.startActivity(shareIntent);
                                break;
                            case PROFILE_DRAWER_ITEM_IDENTIFIER:
                                intent = new Intent(context.getApplicationContext(), ProfileActivity.class);
                                context.startActivity(intent);
                                break;
                            case ABOUT_DRAWER_ITEM_IDENTIFIER:
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://bard.co"));
                                context.startActivity(browserIntent);
                                break;
                            default:
                                break;
                        }

                        // allows drawer to close
                        return false;
                    }
                })
                .build();

    }

    public static void askStoragePermission(final AppCompatActivity context) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//                // Show an expanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
//            }
        }
    }

    public static void askContactsPermission(final AppCompatActivity context) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private static String getRepositoryS3Key(String uuid) {
        return "repositories/" + Setting.getUsername(ClientApp.getContext()) + "/" + uuid + ".mp4";
    }

    public interface OnRepoSaved  {
        void onSaved(Repo repo);
    }

    public interface OnRepoPublished  {
        void onPublished(HashMap<String, String> result);
    }

    public static void saveRemoteRepo(final Repo repo, String uuid, String channelToken, final OnRepoPublished listener) {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("uuid", uuid);
        body.put("word_list", repo.getWordList());
        body.put("scene_token", repo.getSceneToken());
        if (channelToken != null) {
            body.put("channel_token", channelToken);
        }
//        body.put("character_token", this.characterToken);
        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postRepo(body);

        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (!response.isSuccess()) {
                    displayError("Unable to sync to remote server", new Throwable("Failed to save repo to bard server"));
                } else {
                    HashMap<String, String> result = response.body();
                    // uuid, token, url
                    repo.setTokenAndUrl(result.get("token"), result.get("url"));
                    progressDialog.dismiss();
                    listener.onPublished(result);
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                progressDialog.dismiss();
                displayError("Unable to sync to remote server", t);
            }
        });
    }

    public static void publishRepo(final Repo repo, Context context, final String channelToken, final OnRepoPublished listener) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Processing...");
        progressDialog.show();

        final String wordList = repo.getWordList();
        final Scene scene = Scene.forToken(repo.getSceneToken());

        // upload to S3

        final String uuid = UUID.randomUUID().toString();
        AmazonS3 s3 = new AmazonS3Client(AmazonCognito.credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, context.getApplicationContext());
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
                    saveRemoteRepo(repo, uuid, channelToken, listener);
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
                progressDialog.dismiss();
                displayError("Unable to upload to server", ex);
            }

        });
    }

    public static String stringifyTimeShort(int time) {
        int hours = (time / 3600) % 24;
        int minutes = ( time / 60 ) % 60;
        int seconds = time % 60;

        StringBuilder builder = new StringBuilder();
        boolean zeroPrependCheck = false;

        if (hours != 0) {
            builder.append(hours);
            zeroPrependCheck = true;
            builder.append(":");
        }

        if (zeroPrependCheck) {
            builder.append(minutes < 10 ? "0" + minutes : minutes);
        } else {
            builder.append(minutes);
        }
        builder.append(":");

        builder.append(seconds < 10 ? "0" + seconds : seconds);

        return builder.toString();
    }

    public static String getUploadS3Key(String uuid) {
        return "uploads/" + uuid + ".mp4";
    }

    public interface OnUniqueUploadS3KeyGenerated  {
        // This can be any number of events to be sent to the activity
        public void onUniqueUploadS3KeyGenerated(String s3Key);
    }

    public static void generateUniqueUploadS3Key(final AmazonS3 s3, final OnUniqueUploadS3KeyGenerated callback) {
        String s3Key;

        final AsyncTask<String, Integer, String> asyncTask = new AsyncTask<String, Integer, String>() {
            @Override
            protected String doInBackground(String... args) {
                String s3Key;

                while (true) {
                    final String uuid = UUID.randomUUID().toString();
                    s3Key = getUploadS3Key(uuid);

                    if (!s3.doesObjectExist(Configuration.s3UserBucket(), s3Key)) {
                        break;

                    }
                }

                return s3Key;
            }

            @Override
            protected void onPostExecute(String v) {
                callback.onUniqueUploadS3KeyGenerated(v);
            }

        };

        asyncTask.execute("");
    }

    public interface OnUploadComplete {
        public void onUploadComplete(String remoteUrl);
    }


    public static void uploadToS3(final Context context, final File file, final OnUploadComplete callback) {
        final AmazonS3 s3 = new AmazonS3Client(AmazonCognito.credentialsProvider);

        generateUniqueUploadS3Key(s3, new OnUniqueUploadS3KeyGenerated() {
            @Override
            public void onUniqueUploadS3KeyGenerated(final String s3Key) {
                final TransferUtility transferUtility = new TransferUtility(s3, context.getApplicationContext());
                TransferObserver observer = transferUtility.upload(
                        Configuration.s3UserBucket(),
                        s3Key,
                        file
                );

                progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Uploading File...");
                progressDialog.setButton(BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transferUtility.cancelAllWithType(TransferType.UPLOAD);
                        progressDialog.dismiss();
                    }
                });

                progressDialog.show();

                observer.setTransferListener(new TransferListener(){

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        // do something
                        if (state == TransferState.COMPLETED) {
                            progressDialog.dismiss();
                            String remoteUrl = Configuration.s3UserBucketCdnPath() + "/" + s3Key;
                            callback.onUploadComplete(remoteUrl);
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal > 0) {
                            int percentage = (int) ((bytesCurrent/(float) bytesTotal) * 100);
                            //Display percentage transfered to user
                            BardLogger.log("upload progress: " + bytesCurrent + " / " + bytesTotal);
                            progressDialog.setProgress(percentage);
                        } else {
                            progressDialog.setProgress(0);
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        // do something
                        progressDialog.dismiss();
                        displayError("Unable to upload to server", ex);
                    }

                });
            }
        });


    }

    /*
    * based on https://github.com/awslabs/aws-sdk-android-samples/blob/57070662dd95d3b54a00d520857dcc97accedd5d/S3TransferUtilitySample/src/com/amazonaws/demo/s3transferutility/UploadActivity.java
    * Gets the file path of the given Uri.
    */
    @SuppressLint("NewApi")
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[] {
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String getRealPathFromVideoURI(Context context, Uri contentURI) {
        String realPath;
        if (Build.VERSION.SDK_INT < 11) {
            realPath = getRealPathFromURI_BelowAPI11(context, contentURI);

            // SDK >= 11 && SDK < 19
        } else if (Build.VERSION.SDK_INT < 19) {
            realPath = getRealPathFromURI_API11to18(context, contentURI);

            // SDK > 19 (Android 4.4)
        } else {
            realPath = getRealPathFromURI_API19(context, contentURI);
        }

        return realPath;
    }

    @SuppressLint("NewApi")
    public static String getRealPathFromURI_API19(Context context, Uri uri){
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }


    @SuppressLint("NewApi")
    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(
                context,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if(cursor != null){
            int column_index =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        }
        return result;
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri){
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index
                = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




    public interface OnMergeRemoteComplete {
        public void onMergeRemoteComplete(String remoteSourceUrl, String localSourcePath);
    }

    public interface OnDownloadMp4FromRemoteComplete {
        public void onMp4DownlaodComplete(String remoteSourceUrl, String localSourcePath);
    }

    public static long getVideoDuration(Context context, File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(file));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong( time );
        long duration = timeInmillisec / 1000;

        return duration;

//        long hours = duration / 3600;
//        long minutes = (duration - hours * 3600) / 60;
//        long seconds = duration - (hours * 3600 + minutes * 60);

    }

    public static boolean isFileValidMP4(Context context, File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, Uri.fromFile(file));
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        } finally {
            retriever.release();
        }
    }

    public static void downloadRemoteMp4ToLocalPath(Context context, final String remoteSourceUrl, final String localSourcePath, final OnDownloadMp4FromRemoteComplete listener) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        FileOutputStream fileOutput = null;
        try {
            fileOutput = new FileOutputStream(new File(localSourcePath));
            VideoDownloader.downloadUrlToStream(remoteSourceUrl, fileOutput, new VideoDownloader.OnDownloadListener() {
                @Override
                public void onDownloadSuccess() {
                    progressDialog.dismiss();
                    listener.onMp4DownlaodComplete(remoteSourceUrl, localSourcePath);
                }

                @Override
                public void onDownloadFailure() {
                    progressDialog.dismiss();
                    listener.onMp4DownlaodComplete("","");
                }
            });
        } catch (FileNotFoundException e) {
            progressDialog.dismiss();
            e.printStackTrace();
            listener.onMp4DownlaodComplete("","");
        }
    }

    public static void mergeSegmentsRemotely(Context context, String wordList, final OnMergeRemoteComplete listener) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing...");
        progressDialog.show();

        Call<ResponseBody> call = BardClient.getLambdaBardService().lambdaConcat(wordList);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) {
                        progressDialog.dismiss();
                        listener.onMergeRemoteComplete("","");
                        return;
                    }

                    // lambda wraps result in a doublequote, so we have to remove them
                    final String remoteSourceUrl = response.body().string().replace("\"","");

                    final String localSourcePath = Storage.getLocalSavedFilePath();

                    FileOutputStream fileOutput = new FileOutputStream(new File(localSourcePath));
                    VideoDownloader.downloadUrlToStream(remoteSourceUrl, fileOutput, new VideoDownloader.OnDownloadListener() {
                        @Override
                        public void onDownloadSuccess() {
                            progressDialog.dismiss();
                            listener.onMergeRemoteComplete(remoteSourceUrl, localSourcePath);
                        }

                        @Override
                        public void onDownloadFailure() {
                            progressDialog.dismiss();
                            listener.onMergeRemoteComplete("","");
                        }
                    });
                } catch (FileNotFoundException e) {
                    progressDialog.dismiss();
                    e.printStackTrace();
                    listener.onMergeRemoteComplete("","");
                } catch (IOException e) {
                    progressDialog.dismiss();
                    e.printStackTrace();
                    listener.onMergeRemoteComplete("","");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                listener.onMergeRemoteComplete("","");
            }
        });
    }

    public static void saveLocalRepo(String token, String url, String uuid, String localFilePath, String wordList, String sceneToken, String sceneName, String characterToken, OnRepoSaved listener) {

        // set initial token as size of repo + 1
        if (token == null) {
            token = String.valueOf(Repo.getCount() + 1);
        }

        Repo repo;

        repo = Repo.create(token, url, uuid, characterToken, sceneToken, localFilePath, wordList, Calendar.getInstance().getTime());

        Bundle params = new Bundle();
        params.putString("wordTags", wordList);
        params.putString("sceneToken", sceneToken);
        params.putString("scene", sceneName);
        Analytics.track(ClientApp.getContext(), "saveRepo", params);

        listener.onSaved(repo);
    }

    private static void displayError(String message, Throwable t) {
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(t);
    }

    private static void displayError(String message) {
        progressDialog.dismiss();
        Toast.makeText(ClientApp.getContext(), message, Toast.LENGTH_LONG).show();
        CrashReporter.logException(new Throwable(message));
    }

    // http://stackoverflow.com/a/10816846/803865
    public static void openInAppStore(Context context) {
        Uri uri = Uri.parse("market://details?id=com.roplabs.bard");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        if (android.os.Build.VERSION.SDK_INT < 21) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + ClientApp.getContext().getPackageName())));
        }


    }


    public static String truncate(String str, int len) {
        if (str.length() > len) {
            return str.substring(0, len) + "...";
        } else {
            return str;
        }
    }

    public static String formatDate(Date date)  {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        DateFormat timeFormatter = new SimpleDateFormat("hh:mma");
        DateFormat dateFormatter = new SimpleDateFormat("MM-dd-yy");

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return timeFormatter.format(date);
        } else {
            return dateFormatter.format(date);
        }
    }


}
