package com.roplabs.bard.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelPagerAdapter;
import com.roplabs.bard.models.Channel;

/**
 * Created by reg on 2017-03-30.
 */
public class ChannelActivity extends BaseActivity {

    private String channelToken;
    private Channel channel;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        channelToken = intent.getStringExtra("channelToken");
        channel = Channel.forToken(channelToken);


        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText(channel.getName());
        initPager();

    }

    private void initPager() {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.channel_pager);
        viewPager.setAdapter(new ChannelPagerAdapter(getSupportFragmentManager(),
                ChannelActivity.this));
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.channel_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

}
