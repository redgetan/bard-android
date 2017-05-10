package com.roplabs.bard.ui.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;

/**
 * Created by reg on 2017-05-09.
 */
public class ForgotPasswordActivity extends BaseActivity {
    private ProgressDialog progressDialog;
    private EditText emailInput;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);

        emailInput = (EditText) findViewById(R.id.input_email);
        title.setText("Forgot password");
    }

    public void sendPasswordResetLink() {



        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        Call<HashMap<String, String>> call = BardClient.getNonauthenticatedBardService().resetPassword(emailInput.getText().toString());
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                progressDialog.dismiss();
                HashMap<String, String> result = response.body();
                if (result != null) {
                    if (result.get("error") != null) {
                        Toast toast = Toast.makeText(getBaseContext(), result.get("error"), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 50);
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(getBaseContext(), "Passowrd reset link successfully sent", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 50);
                        toast.show();

                        finish();
                    }
                } else {

                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                progressDialog.dismiss();
                Toast toast = Toast.makeText(getBaseContext(), "Unable to send password reset link", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 50);
                toast.show();

            }
        });
    }

    public void onSendLinkBtnClick(View view) {
        sendPasswordResetLink();
    }
}
