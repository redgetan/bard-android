package com.roplabs.bard.adapters;

import android.content.Context;
        import android.support.v4.app.Fragment;
        import android.support.v4.app.FragmentManager;
        import android.support.v4.app.FragmentPagerAdapter;
        import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.ui.fragment.*;
import com.roplabs.bard.util.Helper;

public class BardCreateFragmentPagerAdapter extends FragmentPagerAdapter {
    private String tabTitles[] = new String[] {
            Helper.SCENES,
            Helper.TYPE_TEXT,
            Helper.PACKS
    };
    private Context context;
    private String channelToken;

    public BardCreateFragmentPagerAdapter(FragmentManager fm, Context context) {
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
        if (sceneType.equals(Helper.SCENES)) {
            return MainSceneSelectFragment.newInstance();
        } else {
            return PackSelectFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.PACKS)) {
            return "Use Pack";
        } else if (tabTitles[position].equals(Helper.SCENES)) {
            return "Pick Video";
        } else {
            return "Type Text";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
