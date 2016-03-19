package com.sandbox.myfirstapp.app.ui;

import android.content.Intent;
import android.os.Bundle;
import com.orm.SugarRecord;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.Repo;

import java.util.Calendar;

public class SaveActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra(MyActivity.EXTRA_VIDEO_URL);
        String videoPath = intent.getStringExtra(MyActivity.EXTRA_VIDEO_PATH);
        String wordList = intent.getStringExtra(MyActivity.EXTRA_WORD_LIST);

        saveRepo(videoUrl, videoPath, wordList);

    }

    private void saveRepo(String videoUrl, String videoPath, String wordList) {
        Repo repo = new Repo(videoUrl, videoPath, wordList, Calendar.getInstance().getTime());
        SugarRecord.save(repo);
    }
}
