package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.ui.activity.SceneSelectActivity;
import com.roplabs.bard.ui.fragment.*;
import com.roplabs.bard.util.Helper;

public class SceneSelectFragmentPagerAdapter extends FragmentPagerAdapter {
    private String tabTitles[] = new String[] {
            Helper.CHANNEL_FEED,
            Helper.BARD_CREATE,
            Helper.PROFILE
    };
    private Context context;
    private String channelToken;

    public SceneSelectFragmentPagerAdapter(FragmentManager fm, Context context, String channelToken) {
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
        String sceneType = tabTitles[position];
        if (sceneType.equals(Helper.CHANNEL_FEED)) {
            return ChannelFeedFragment.newInstance(channelToken);
        } else if (sceneType.equals(Helper.BARD_CREATE)) {
            return BardCreateFragment.newInstance();
        } else {
            return ProfileFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.CHANNEL_FEED)) {
            return "Feed";
        } else if (tabTitles[position].equals(Helper.ONLINE_LIBRARY)) {
            return "Videos";
        } else {
            return "Packs";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
