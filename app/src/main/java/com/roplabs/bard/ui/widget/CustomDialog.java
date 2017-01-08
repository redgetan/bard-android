package com.roplabs.bard.ui.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.ui.activity.LoginActivity;
import com.roplabs.bard.ui.activity.SignupActivity;

// http://stackoverflow.com/a/13342157
public class CustomDialog extends Dialog implements
        android.view.View.OnClickListener {

        public Activity activity;
        public Button loginBtn, registerBtn;
        public ImageView cancelBtn;
        String message;

        public static final int LOGIN_REQUEST_CODE = 3;
        public static final int SIGNUP_REQUEST_CODE = 4;

        public CustomDialog(Activity activity, String message) {
            super(activity);
            this.activity = activity;
            this.message = message;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.fragment_custom_dialog);
            loginBtn = (Button) findViewById(R.id.login_btn);
            registerBtn = (Button) findViewById(R.id.register_btn);
            cancelBtn = (ImageView) findViewById(R.id.cancel_btn);
            TextView dialogDescription = (TextView) findViewById(R.id.dialog_description);
            dialogDescription.setText(message);

            loginBtn.setBackgroundColor(ContextCompat.getColor(activity, R.color.purple));
            registerBtn.setBackgroundColor(ContextCompat.getColor(activity, R.color.purple));

            loginBtn.setOnClickListener(this);
            registerBtn.setOnClickListener(this);
            cancelBtn.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.login_btn:
                    intent = new Intent(activity, LoginActivity.class);
                    activity.startActivityForResult(intent, LOGIN_REQUEST_CODE);
                    break;
                case R.id.register_btn:
                    intent = new Intent(activity, SignupActivity.class);
                    activity.startActivityForResult(intent, SIGNUP_REQUEST_CODE);
                    break;
                case R.id.cancel_btn:
                    dismiss();
                    break;
                default:
                    break;
            }
        }
}
