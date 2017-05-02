package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.CustomDialog;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.CrashReporter;
import com.roplabs.bard.util.Helper;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.roplabs.bard.util.Helper.LOGIN_REQUEST_CODE;

public class UploadVideoActivity extends BaseActivity {

    private TextView uploadResultMessage;
    private EditText uploadVideoInput;
    private Button uploadVideoButton;
    private Button chooseFileButton;
    private CustomDialog loginDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Upload Video");

        uploadResultMessage = (TextView) findViewById(R.id.upload_result_message);
        uploadResultMessage.setVisibility(View.GONE);
        uploadVideoButton    = (Button) findViewById(R.id.upload_video_button);
        uploadVideoInput    = (EditText) findViewById(R.id.upload_video_input);
        chooseFileButton   = (Button) findViewById(R.id.choose_file_upload_button);

        uploadVideoInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    importFromYoutube(null);
                    return true;
                }
                return false;
            }
        });

        chooseFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadVideoFromFile(v);
            }
        });

    }

    public void uploadVideoFromFile(View view) {
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to upload a video");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= 19) {
            // For Android KitKat, we use a different intent to ensure
            // we can
            // get the file path from the returned intent URI
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setType("*/*");
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
        }

        startActivityForResult(intent, Helper.CHOOSE_FILE_UPLOAD_REQUEST_CODE);
    }

    public void importFromYoutube(View view) {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to upload a video");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        uploadVideoButton.setEnabled(false);

        final String youtubeUrl = uploadVideoInput.getText().toString().trim();

        HashMap<String, String> options = new HashMap<String, String>();
        options.put("youtube_url", youtubeUrl);

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postUploadVideo(options);

        handleVideoWordMapRequest(call, new OnVideoWordMapComplete() {
            @Override
            public void onVideoWordMapComplete() {
                Bundle params = new Bundle();
                params.putString("youtubeUrl", youtubeUrl);
                Analytics.track(ClientApp.getContext(), "uploadVideo", params);
            }
        });

    }

    public interface OnVideoWordMapComplete {
        public void onVideoWordMapComplete();
    }

    public void handleVideoWordMapRequest(Call<HashMap<String, String>> call, final OnVideoWordMapComplete callback) {
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> result = response.body();

                uploadResultMessage.setVisibility(View.VISIBLE);

                if (result == null) {
                    uploadResultMessage.setText("Unable to upload video. Something went wrong.");
                    uploadResultMessage.setTextColor(ContextCompat.getColor(ClientApp.getContext(), R.color.md_red_300));
                } else if (!response.isSuccess() || result.get("error") != null) {
                    uploadResultMessage.setText(result.get("error"));
                    uploadResultMessage.setTextColor(ContextCompat.getColor(ClientApp.getContext(), R.color.md_red_300));
                } else {
                    uploadResultMessage.setText(result.get("result"));
                    uploadResultMessage.setTextColor(ContextCompat.getColor(ClientApp.getContext(), R.color.md_green_300));

                    callback.onVideoWordMapComplete();
                }

                uploadVideoButton.setEnabled(true);
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                uploadResultMessage.setVisibility(View.VISIBLE);
                uploadResultMessage.setText("Unable to upload video. Something went wrong.");
                uploadResultMessage.setTextColor(ContextCompat.getColor(ClientApp.getContext(), R.color.md_red_300));
                uploadVideoButton.setEnabled(true);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Context self = this;
        if (resultCode == RESULT_OK && requestCode == CustomDialog.LOGIN_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(ClientApp.getContext(), "Login successful", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == CustomDialog.SIGNUP_REQUEST_CODE) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    loginDialog.dismiss();
                    Toast.makeText(ClientApp.getContext(), "Account successfully created", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(r, 200);
        } else if (resultCode == RESULT_OK && requestCode == Helper.CHOOSE_FILE_UPLOAD_REQUEST_CODE) {
            Uri selectedMediaUri = data.getData();

            if (selectedMediaUri.toString().contains("video")) {
                try {
                    final File file = new File(Helper.getPath(this, selectedMediaUri));
                    Helper.uploadToS3(this, file, new Helper.OnUploadComplete() {
                        @Override
                        public void onUploadComplete(String remoteUrl) {
                            String name = file.getName();
                            long duration = Helper.getVideoDuration(self, file);
                            postRemoteVideoToServer(remoteUrl, name, duration);
                        }
                    });
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                // show alert
                Toast.makeText(ClientApp.getContext(), "Can only upload videos with .mp4 extension", Toast.LENGTH_LONG).show();
            }

        }
    }

    public void postRemoteVideoToServer(final String sourceUrl, final String name, final long duration) {
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("source_url", sourceUrl);
        options.put("name", name);
        options.put("duration", String.valueOf(duration));

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postUploadVideo(options);

        handleVideoWordMapRequest(call, new OnVideoWordMapComplete() {
            @Override
            public void onVideoWordMapComplete() {
                Bundle params = new Bundle();
                params.putString("sourceUrl", sourceUrl);
                params.putString("name", name);
                params.putString("duration", String.valueOf(duration));
                Analytics.track(ClientApp.getContext(), "uploadVideo", params);
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // click on 'up' button in the action bar, handle it here
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
