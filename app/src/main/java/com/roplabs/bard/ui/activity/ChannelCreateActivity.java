package com.roplabs.bard.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.BardLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;

import static com.roplabs.bard.ClientApp.getContext;
import static com.roplabs.bard.util.Helper.CHANNEL_MEMBER_INVITE_REQUEST_CODE;

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
        title.setText("New Group");

        channelNameInput = (EditText) findViewById(R.id.input_channel_name);
        channelNameDescription = (EditText) findViewById(R.id.input_channel_description);
        createChannelButton = (Button) findViewById(R.id.btn_create_channel);

    }


    public void onChannelCreate(View view) {
        String channelName = channelNameInput.getText().toString();

        if (channelName.isEmpty()) {
            Toast.makeText(getContext(), "Group Name cant be blank", Toast.LENGTH_LONG).show();
            return;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", channelName);
        map.put("participants", Setting.getUsername(ClientApp.getContext()));
        map.put("is_private", "true");
        map.put("mode", "group");

        final Context self = this;

        Call<Channel> call = BardClient.getAuthenticatedBardService().createChannel(map);
        call.enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {

                if (response.code() != 200) {
                    return;
                }

                Channel channel = response.body();

                Intent intent = new Intent(self, ChannelMemberInviteActivity.class);
                intent.putExtra("channelToken", channel.getToken());
                startActivityForResult(intent, CHANNEL_MEMBER_INVITE_REQUEST_CODE);
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {
                Toast.makeText(getContext(), "Unable to create channel", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == CHANNEL_MEMBER_INVITE_REQUEST_CODE)) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
