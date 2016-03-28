package com.roplabs.madchat.ui;

import android.content.Intent;
import android.os.Bundle;
import com.roplabs.madchat.R;
import com.roplabs.madchat.models.Repo;

import java.util.Calendar;

public class SaveActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        Intent intent = getIntent();
        String token = intent.getStringExtra(MyActivity.EXTRA_REPO_TOKEN);
        String videoUrl = intent.getStringExtra(MyActivity.EXTRA_VIDEO_URL);
        String videoPath = intent.getStringExtra(MyActivity.EXTRA_VIDEO_PATH);
        String wordList = intent.getStringExtra(MyActivity.EXTRA_WORD_LIST);

        Repo.create(token, videoUrl, videoPath, wordList, Calendar.getInstance().getTime());

    }

}
