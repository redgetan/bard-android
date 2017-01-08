package com.roplabs.bard.ui.activity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.instabug.library.Instabug;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.BardLogger;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;

public class UploadVideoActivity extends BaseActivity {

    private TextView uploadResultMessage;
    private EditText uploadVideoInput;
    private Button uploadVideoButton;

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

                    JSONObject properties = new JSONObject();
                    try {
                        properties.put("youtubeUrl", youtubeUrl);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Instabug.reportException(e);
                    }

                    Analytics.track(ClientApp.getContext(), "uploadVideo", properties);

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
}
