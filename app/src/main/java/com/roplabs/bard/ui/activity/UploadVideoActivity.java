package com.roplabs.bard.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.roplabs.bard.R;

public class UploadVideoActivity extends BaseActivity {

    private TextView uploadResultMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Upload Video");

        uploadResultMessage = (TextView) findViewById(R.id.upload_result_message);

    }

    public void uploadVideo(View view) {
    }
}
