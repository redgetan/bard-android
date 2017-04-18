package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.ui.fragment.BardCreateFragment;
import com.roplabs.bard.ui.fragment.ChannelFeedFragment;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.ui.fragment.SearchResultFragment;
import com.roplabs.bard.util.Helper;

public class SceneSelectFragmentPagerAdapter extends FragmentPagerAdapter {
    private String tabTitles[] = new String[] {
            Helper.BARD_CREATE,
            Helper.CHANNEL_FEED,
            Helper.MY_VIDEOS
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
            return BardCreateFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.CHANNEL_FEED)) {
            return "Channels";
        } else if (tabTitles[position].equals(Helper.BARD_CREATE)) {
            return "Create";
        } else {
            return "Upload";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
