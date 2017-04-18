package com.roplabs.bard.ui.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mikepenz.materialdrawer.view.BezelImageView;
import com.roplabs.bard.BuildConfig;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Like;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.LoginActivity;
import com.roplabs.bard.ui.activity.MainActivity;
import com.roplabs.bard.ui.activity.RepoListActivity;
import com.roplabs.bard.util.Helper;

import java.util.List;

/**
 * Created by reg on 2017-04-18.
 */
public class ProfileFragment extends Fragment {

    private final int LOGIN_REQUEST_CODE = 2;
    private final int NUM_OF_ROW_ITEMS = 11;

    public static ProfileFragment newInstance() {
        Bundle args = new Bundle();
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initProfileHeader(view);
        initProfileDetails(view);

        return view;
    }

    private void initProfileHeader(View view) {
        final AppCompatActivity context = (AppCompatActivity) getActivity();
        BezelImageView profileImageView = (BezelImageView) view.findViewById(R.id.profile_icon);
        TextView profileUsername = (TextView) view.findViewById(R.id.profile_username);
        TextView profileEmail = (TextView) view.findViewById(R.id.profile_email);

        assert profileUsername != null;
        assert profileEmail != null;

        String username = Setting.getUsername(ClientApp.getContext());
        if (username.equals("anonymous")) {
            profileUsername.setText(R.string.click_to_login);
            profileUsername.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!Setting.isLogined(context)) {
                        Intent intent = new Intent(context, LoginActivity.class);
                        context.startActivityForResult(intent, LOGIN_REQUEST_CODE);
                    }
                }
            });
        } else {
            profileUsername.setText(username);
        }

        profileEmail.setText(Setting.getEmail(ClientApp.getContext()));
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Setting.isLogined(ClientApp.getContext())) {
                    Intent intent = new Intent(context, LoginActivity.class);
                    startActivityForResult(intent, LOGIN_REQUEST_CODE);
                }
            }
        });
    }


    public void sendGmail() {
        Intent email = new Intent();
        email.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        email.putExtra(Intent.EXTRA_EMAIL, new String[] { "info@bard.co" });
        email.putExtra(Intent.EXTRA_SUBJECT, "Bard Feedback " + BuildConfig.VERSION_NAME);
        email.putExtra(Intent.EXTRA_TEXT, "It would be awesome if ...");
        startActivity(email);
    }

    public void sendRegularMail() {
        Intent Email = new Intent(Intent.ACTION_SEND);
        Email.setType("text/email");
        Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "info@bard.co" });
        Email.putExtra(Intent.EXTRA_SUBJECT, "Bard Feedback " + BuildConfig.VERSION_NAME);
        Email.putExtra(Intent.EXTRA_TEXT, "It would be awesome if ...");
        startActivity(Intent.createChooser(Email, "Send Feedback:"));
    }

    private void initProfileDetails(View view) {
        final Context self = getActivity();
        ViewGroup container = (ViewGroup) view.findViewById(R.id.profile_details_container);
        LinearLayout.LayoutParams params;
        assert container != null;

        final List<Repo> repos = Repo.forUsername(Setting.getUsername(ClientApp.getContext()));
        final List<Like> userLikes = Like.forUsername(Setting.getUsername(ClientApp.getContext()));
        String bardCount = String.valueOf(repos.size());
        String likeCount = String.valueOf(userLikes.size());

        int numRows = Setting.isLogined(ClientApp.getContext()) ? NUM_OF_ROW_ITEMS : NUM_OF_ROW_ITEMS - 1; // if not logged in, dont show last row (logout)

        for (int i = 0; i < numRows; i++) {
            View profileRow = getActivity().getLayoutInflater().inflate(R.layout.profile_row_item, null);
            TextView textView = (TextView) profileRow.findViewById(R.id.profile_detail_text);

            switch (i) {
                case 0:
                    // My Bards
                    textView.setText("My Bards (" + bardCount + ")");

                    profileRow.findViewById(R.id.profile_navigation_icon).setVisibility(View.VISIBLE);

                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), RepoListActivity.class);
                            intent.putExtra("repoListType","created");
                            startActivity(intent);
                        }
                    });
                    break;
                case 1:
                    // My Likes
                    textView.setText("My Likes (" + likeCount + ")");

                    profileRow.findViewById(R.id.profile_navigation_icon).setVisibility(View.VISIBLE);

                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), RepoListActivity.class);
                            intent.putExtra("repoListType","likes");
                            startActivity(intent);
                        }
                    });
                    break;
                case 2:
                    textView.setText(R.string.feedback);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 25, 0, 0);
                    profileRow.setLayoutParams(params);

                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                sendGmail();
                            } catch(ActivityNotFoundException ex) {
                                sendRegularMail();
                            } catch (SecurityException ex) {
                                sendRegularMail();
                            }
                        }
                    });
                    break;
                case 3:
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
                case 4:
                    textView.setText("Rate this App");
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Helper.openInAppStore(ClientApp.getContext());
                        }
                    });
                    break;
                case 5:
                    textView.setText(R.string.follow_facebook);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/letsbard"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 6:
                    textView.setText(R.string.follow_twitter);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/letsbard"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 7:
                    textView.setText(R.string.about);

                    params = new LinearLayout.LayoutParams(
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
                case 8:
                    textView.setText(R.string.privacy_policy);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co/privacy"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 9:
                    textView.setText(R.string.terms_of_use);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bard.co/terms"));
                            startActivity(browserIntent);
                        }
                    });
                    break;
                case 10:
                    textView.setText(R.string.logout);
                    profileRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Setting.clearUserCredentials(ClientApp.getContext());

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

        try {
            String versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            TextView versionLabel = new TextView(getActivity());
            versionLabel.setText("Version " + versionName);

            ViewGroup.LayoutParams viewGroupParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            versionLabel.setLayoutParams(viewGroupParams);
            versionLabel.setPadding(0, 10, 0, 0);
            versionLabel.setGravity(Gravity.CENTER_HORIZONTAL);

            container.addView(versionLabel);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
