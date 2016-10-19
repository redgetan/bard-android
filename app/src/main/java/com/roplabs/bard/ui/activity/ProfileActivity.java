package com.roplabs.bard.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.instabug.library.Instabug;
import com.mikepenz.materialdrawer.view.BezelImageView;
import com.roplabs.bard.ClientApp;
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
        initProfileDetails();

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

        String username = Setting.getUsername(this);
        if (username.equals("anonymous")) {
            profileUsername.setText(R.string.click_to_login);
        } else {
            profileUsername.setText(username);
        }

        profileEmail.setText(Setting.getEmail(this));
    }

    // http://stackoverflow.com/a/10816846/803865
    private void openInAppStore() {
        Uri uri = Uri.parse("market://details?id=" + ClientApp.getContext().getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        if (android.os.Build.VERSION.SDK_INT < 21) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + ClientApp.getContext().getPackageName())));
        }
    }

    private void initProfileDetails() {
        final Context self = this;
        ViewGroup container = (ViewGroup) findViewById(R.id.profile_details_container);
        assert container != null;

        int numRows = Setting.isLogined(this) ? 7 : 6; // if not logged in, dont show last row (logout)

        for (int i = 0; i <= numRows; i++) {
            View profileRow = getLayoutInflater().inflate(R.layout.profile_row_item, null);
            TextView textView = (TextView) profileRow.findViewById(R.id.profile_detail_text);

            switch (i) {
                case 0:
                    textView.setText(R.string.feedback);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Instabug.invoke();
                        }
                    });
                    break;
                case 1:
                    textView.setText(R.string.tell_friend);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, you should check out https://bard.co");
                            shareIntent.setType("text/plain");
                            startActivity(shareIntent);
                        }
                    });
                    break;
                case 2:
                    textView.setText("Rate this App");
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openInAppStore();
                        }
                    });
                    break;
                case 3:
                    textView.setText(R.string.follow_facebook);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/letsbard"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 4:
                    textView.setText(R.string.follow_twitter);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/letsbard"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 5:
                    textView.setText(R.string.about);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 25, 0, 0);
                    profileRow.setLayoutParams(params);

                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 6:
                    textView.setText(R.string.privacy_policy);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co/privacy"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 7:
                    textView.setText(R.string.terms_of_use);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co/terms"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 8:
                    textView.setText(R.string.logout);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Setting.clearUserCredentials(getApplicationContext());

                            Intent intent = new Intent(self, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    });
                    break;
                default:
                    break;
            }

            container.addView(profileRow); // you can pass extra layout params here too
        }
    }

}
