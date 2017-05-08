package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.ui.activity.SceneSelectActivity;
import com.roplabs.bard.ui.fragment.*;
import com.roplabs.bard.util.Helper;

import java.util.Arrays;

public class SimpleSceneSelectFragmentPagerAdapter extends FragmentPagerAdapter {
    public static String tabTitles[] = new String[] {
            Helper.BARD_CREATE
    };
    private Context context;
    private String channelToken;

    public SimpleSceneSelectFragmentPagerAdapter(FragmentManager fm, Context context, String channelToken) {
        super(fm);
        this.context = context;
        this.channelToken = channelToken;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    public static int getBardCreateFragmentPosition() {
        return Arrays.asList(tabTitles).indexOf(Helper.BARD_CREATE);
    }

    @Override
    public Fragment getItem(int position) {
        return BardCreateFragment.newInstance();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "Videos";
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
