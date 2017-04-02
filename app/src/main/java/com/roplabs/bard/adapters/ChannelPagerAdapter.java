package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.ui.fragment.ChannelFeedFragment;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.util.Helper;

public class ChannelPagerAdapter extends FragmentPagerAdapter {
    private String tabTitles[] = new String[] {
            Helper.CHANNEL_FEED,
            Helper.CHANNEL_VIDEOS
    };
    private Context context;
    private String channelToken;

    public ChannelPagerAdapter(FragmentManager fm, Context context, String channelToken) {
        super(fm);
        this.context = context;
        this.channelToken = channelToken;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public Fragment getItem(int position) {
        if (tabTitles[position].equals(Helper.CHANNEL_FEED)) {
            return ChannelFeedFragment.newInstance();
        } else if (tabTitles[position].equals(Helper.CHANNEL_VIDEOS)) {
            return SceneSelectFragment.newInstance(Helper.CHANNEL_VIDEOS, channelToken);
        } else {
            return SceneSelectFragment.newInstance(Helper.CHANNEL_VIDEOS, channelToken);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.CHANNEL_FEED)) {
            return "Posts";
        } else if (tabTitles[position].equals(Helper.CHANNEL_VIDEOS)) {
            return "Videos";
        } else {
            return "";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
