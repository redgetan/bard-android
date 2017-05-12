package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.config.Configuration;
import org.w3c.dom.Text;

/**
 * Created by reg on 2017-05-12.
 */
public class ChannelMemberInviteActivity extends BaseActivity {
    private String channelToken;
    private TextView channelInviteLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_member_invite);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channelInviteLink = (TextView) findViewById(R.id.channel_invite_link);
        channelInviteLink.setText(Configuration.bardAPIBaseURL() + "/channels/" + channelToken);
    }

    @Override
    public void onBackPressed() {
        // go to home
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel_member_invite, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_item_channel_invite_finish:
                // must set usernames on intent to let these people join
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
