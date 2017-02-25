package com.roplabs.bard.util;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
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
import com.roplabs.bard.models.AmazonCognito;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.*;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.support.v7.widget.Toolbar;

import java.io.*;
import java.util.*;


public class Helper {

    public static final int CREATE_DRAWER_ITEM_IDENTIFIER = 1;
    public static final int MY_PROJECTS_DRAWER_ITEM_IDENTIFIER = 2;
    public static final int ABOUT_DRAWER_ITEM_IDENTIFIER = 3;
    public static final int UPLOAD_VIDEO_DRAWER_ITEM_IDENTIFIER = 4;
    public static final int PROFILE_DRAWER_ITEM_IDENTIFIER = 5;
    public static final int TELL_FRIEND_DRAWER_ITEM_IDENTIFIER = 6;

    public static final int REQUEST_WRITE_STORAGE = 1;
    public static final int LOGIN_REQUEST_CODE = 2;
    public static final int PROFILE_REQUEST_CODE = 3;
    public static final int SHARE_REPO_REQUEST_CODE = 4;
    public static final int SEARCH_REQUEST_CODE = 5;
    public static final int SHARE_SCENE_REQUEST_CODE = 6;

    public static final String POPULAR_SCENE_TYPE = "top";
    public static final String NEW_SCENE_TYPE = "latest";
    public static final String FAVORITES_SCENE_TYPE = "favorites";


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



    // http://gimite.net/en/index.php?Run%20native%20executable%20in%20Android%20App
    public static String runCmd(String[] cmd) {
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec(cmd);

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

    public static void initNavigationViewDrawer(final AppCompatActivity context, Toolbar toolbar) {
        String username = Setting.getUsername(context);
        ProfileDrawerItem profileDrawerItem;
        final List<Repo> repos = Repo.forUsername(Setting.getUsername(context));
        String libraryCount = String.valueOf(repos.size());

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
                        new PrimaryDrawerItem().withName(R.string.bard_library).withTextColor(textColor).withIdentifier(MY_PROJECTS_DRAWER_ITEM_IDENTIFIER).withIcon(R.drawable.ic_inbox_black_24dp).withBadge(libraryCount).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.jumbo)),
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

    private static String getRepositoryS3Key(String uuid) {
        return "repositories/" + Setting.getUsername(ClientApp.getContext()) + "/" + uuid + ".mp4";
    }

    public interface OnRepoSaved  {
        void onSaved(Repo repo);
    }

    public interface OnRepoPublished  {
        void onPublished(Repo repo);
    }

//    public static void saveRepo(Context context, String wordTagListString, final String sceneToken, final String sceneName, final OnRepoSaved listener) {
//        progressDialog = new ProgressDialog(context);
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.setMessage("Saving...");
//        progressDialog.show();
//
//        final String wordList = wordTagListString;
//
//        // upload to S3
//
//        final String uuid = UUID.randomUUID().toString();
//        AmazonS3 s3 = new AmazonS3Client(AmazonCognito.credentialsProvider);
//        TransferUtility transferUtility = new TransferUtility(s3, context.getApplicationContext());
//        TransferObserver observer = transferUtility.upload(
//                Configuration.s3UserBucket(),
//                getRepositoryS3Key(uuid),
//                new File(Storage.getMergedOutputFilePath())
//        );
//
//        observer.setTransferListener(new TransferListener(){
//
//            @Override
//            public void onStateChanged(int id, TransferState state) {
//                // do something
//                if (state == TransferState.COMPLETED) {
//                    saveRemoteRepo(uuid, wordList, sceneToken, sceneName, listener);
//                }
//            }
//
//            @Override
//            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
//                int percentage = (int) (bytesCurrent/bytesTotal * 100);
//                //Display percentage transfered to user
//            }
//
//            @Override
//            public void onError(int id, Exception ex) {
//                // do something
//                displayError("Unable to upload to server", ex);
//            }
//
//        });
//
//    }

    private static void saveRemoteRepo(final Repo repo, String uuid, final OnRepoPublished listener) {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("uuid", uuid);
        body.put("word_list", repo.getWordList());
        body.put("scene_token", repo.getSceneToken());
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
                    listener.onPublished(repo);
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                progressDialog.dismiss();
                displayError("Unable to sync to remote server", t);
            }
        });
    }

    public static void publishRepo(final Repo repo, Context context, final OnRepoPublished listener) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Publishing...");
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
                    saveRemoteRepo(repo, uuid, listener);
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

    public static void saveLocalRepo(String token, String url, String wordList, String sceneToken, String sceneName, OnRepoSaved listener) {

        // set initial token as size of repo + 1
        if (token == null) {
            token = String.valueOf(Repo.getCount() + 1);
        }

        String filePath = Storage.getLocalSavedFilePath();
        Repo repo;

        if (Helper.copyFile(Storage.getMergedOutputFilePath(),filePath)) {
            repo = Repo.create(token, url, "", sceneToken, filePath, wordList, Calendar.getInstance().getTime());

            JSONObject properties = new JSONObject();
            Bundle params = new Bundle();
            try {
                properties.put("wordTags", wordList);
                properties.put("sceneToken", sceneToken);
                properties.put("scene", sceneName);

                params.putString("wordTags", wordList);
                params.putString("sceneToken", sceneToken);
                params.putString("scene", sceneName);
            } catch (JSONException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
            Analytics.track(ClientApp.getContext(), "saveRepo", properties);
            Analytics.track(ClientApp.getContext(), "saveRepo", params);

            listener.onSaved(repo);

        } else {
            displayError("Unable to save to phone");
        }
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




}
