package com.roplabs.madchat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.roplabs.madchat.R;
import com.roplabs.madchat.models.Index;
import com.roplabs.madchat.models.Setting;


public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent;
        String authToken = Setting.getAuthenticationToken(this);

        if (authToken.length() > 0) {
            intent = new Intent(this, IndexActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
    }

}
