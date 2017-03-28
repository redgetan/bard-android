package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.ui.fragment.SearchResultFragment;
import com.roplabs.bard.util.Helper;

public class SceneSelectFragmentPagerAdapter extends FragmentPagerAdapter {
    private String tabTitles[] = new String[] {
            Helper.ONLINE_LIBRARY,
            Helper.MY_VIDEOS
    };
    private Context context;

    public SceneSelectFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public Fragment getItem(int position) {
        String sceneType = tabTitles[position];
        return SceneSelectFragment.newInstance(sceneType, position + 1);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.MY_VIDEOS)) {
            return "My Videos";
        } else if (tabTitles[position].equals(Helper.ONLINE_LIBRARY)) {
            return "All";
        } else {
            return "All";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
