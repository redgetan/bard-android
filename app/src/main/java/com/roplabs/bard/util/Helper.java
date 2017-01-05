package com.roplabs.bard.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import com.roplabs.bard.ClientApp;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Response;

import java.io.*;
import java.util.Locale;

public class Helper {

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
}
