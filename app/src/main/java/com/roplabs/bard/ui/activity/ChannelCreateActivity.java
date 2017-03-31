package com.roplabs.bard.ui.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.util.BardLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;

import static com.roplabs.bard.ClientApp.getContext;

public class ChannelCreateActivity extends BaseActivity{

    private Button createChannelButton;
    private EditText channelNameInput;
    private EditText channelNameDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("ChannelCreate onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_create);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Create your Channel");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) { actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp); }

        channelNameInput = (EditText) findViewById(R.id.input_channel_name);
        channelNameDescription = (EditText) findViewById(R.id.input_channel_description);
        createChannelButton = (Button) findViewById(R.id.btn_create_channel);

    }


    public void onChannelCreate(View view) {

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", channelNameInput.getText().toString());
        map.put("description", channelNameDescription.getText().toString());

        Call<Channel> call = BardClient.getAuthenticatedBardService().createChannel(map);
        call.enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {

                if (response.code() != 200) {
                    return;
                }

                Channel remoteChannel = response.body();
                Channel.create(remoteChannel);

                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {
                Toast.makeText(getContext(), "Unable to create channel", Toast.LENGTH_LONG).show();
            }
        });

    }
}
