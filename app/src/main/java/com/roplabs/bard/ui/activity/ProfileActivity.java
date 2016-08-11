package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;
import com.instabug.library.Instabug;
import com.mikepenz.materialdrawer.view.BezelImageView;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Setting;
import org.w3c.dom.Text;

// http://stackoverflow.com/a/34760299/803865
public class ProfileActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(R.string.settings_string);

        initProfileHeader();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        assert fab != null;
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    private void initProfileHeader() {
        BezelImageView profileImageView = (BezelImageView) findViewById(R.id.profile_icon);
        TextView profileUsername = (TextView) findViewById(R.id.profile_username);
        TextView profileEmail = (TextView) findViewById(R.id.profile_email);

        assert profileUsername != null;
        assert profileEmail != null;

        profileUsername.setText(Setting.getUsername(this));
        profileEmail.setText(Setting.getEmail(this));
    }

    public void onFeedbackRowClick(View view) {
        Instabug.invoke();
    }

    public void onTellFriendRowClick(View view) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, you should check out https://bard.co");
        shareIntent.setType("text/plain");
        startActivity(shareIntent);
    }

    public void onFollowFacebookRowClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/letsbard"));
        startActivity(browserIntent);
    }

    public void onFollowTwitterRowClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/letsbard"));
        startActivity(browserIntent);
    }

    public void onAboutRowClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co"));
        startActivity(browserIntent);
    }

    public void onPrivacyRowClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co/privacy"));
        startActivity(browserIntent);
    }

    public void onLogoutRowClick(View view) {
        Setting.clearUserCredentials(getApplicationContext());

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

}
