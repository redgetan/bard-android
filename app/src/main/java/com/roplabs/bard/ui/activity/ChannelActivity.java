package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Channel;

/**
 * Created by reg on 2017-03-30.
 */
public class ChannelActivity extends BaseActivity {

    private String channelToken;
    private Channel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channel = Channel.forToken(channelToken);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(channel.getName());
    }

}
