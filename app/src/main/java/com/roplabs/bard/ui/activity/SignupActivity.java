package com.roplabs.bard.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.Bind;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.User;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.Helper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private ProgressDialog progressDialog;

    @Bind(R.id.input_name) EditText _nameText;
    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_signup) Button _signupButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        TextView agreeToTermsTextView = (TextView) findViewById(R.id.agree_to_terms);
        agreeToTermsTextView.setText(Html.fromHtml(getString(R.string.agree_to_terms_description)));
        Linkify.addLinks(agreeToTermsTextView, Linkify.ALL);
        agreeToTermsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
    }

    public void signup() {
        if (!validate()) {
            return;
        }

        _signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String username = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        final Context self = this;

        Call<User> call = BardClient.getNonauthenticatedBardService().signUp(new User(username, email, password));
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progressDialog.dismiss();
                if (!response.isSuccess()) {
                    String error = Helper.parseError(response);
                    displayError(error);
                } else {
                    User user = response.body();
                    Setting.setUserCredentials(self, user);
                    Analytics.identify(self, Calendar.getInstance().getTime());
                    _signupButton.setEnabled(true);
                    setResult(RESULT_OK, null);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                progressDialog.dismiss();
                displayError("timeout");
            }
        });

    }

    private void displayError(String message) {
        Toast toast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 10);
        toast.show();
        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (name.isEmpty()) {
            _nameText.setError("cant be empty");
            valid = false;
        } else if (!name.matches("\\A[a-z0-9_]+\\z")) {
            _nameText.setError("can only contain a-z, 0-9, and _");
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
