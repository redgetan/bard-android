package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;

import static com.roplabs.bard.util.Helper.LOGIN_REQUEST_CODE;

public class UploadVideoActivity extends BaseActivity {

    private TextView uploadResultMessage;
    private EditText uploadVideoInput;
    private Button uploadVideoButton;
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

        uploadVideoInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    uploadVideo(null);
                    return true;
                }
                return false;
            }
        });

    }

    public void uploadVideo(View view) {
        // cant upload unless you're loggedin
        if (!Setting.isLogined(this)) {
            loginDialog = new CustomDialog(this, "You must login to upload a video");
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loginDialog.show();
            return;
        }

        uploadVideoButton.setEnabled(false);

        final String youtubeUrl = uploadVideoInput.getText().toString().trim();

        Call<HashMap<String, String>> call = BardClient.getAuthenticatedBardService().postUploadVideo(youtubeUrl);

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

                    Bundle params = new Bundle();
                    params.putString("youtubeUrl", youtubeUrl);
                    Analytics.track(ClientApp.getContext(), "uploadVideo", params);

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
        }
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
