package com.roplabs.bard.ui;

import android.content.Intent;
import android.os.Bundle;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Repo;

import java.util.Calendar;

public class SaveActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        Intent intent = getIntent();
        String token = intent.getStringExtra(MimicActivity.EXTRA_REPO_TOKEN);
        String videoUrl = intent.getStringExtra(MimicActivity.EXTRA_VIDEO_URL);
        String videoPath = intent.getStringExtra(MimicActivity.EXTRA_VIDEO_PATH);
        String wordList = intent.getStringExtra(MimicActivity.EXTRA_WORD_LIST);

        Repo.create(token, videoUrl, videoPath, wordList, Calendar.getInstance().getTime());

    }

}
