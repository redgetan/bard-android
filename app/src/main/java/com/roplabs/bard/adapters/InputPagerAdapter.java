package com.roplabs.bard.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.roplabs.bard.ui.fragment.VideoResultFragment;
import com.roplabs.bard.ui.fragment.WordListFragment;

public class InputPagerAdapter extends SmartFragmentStatePagerAdapter {
    private static int NUM_ITEMS = 2;

    public InputPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: // Fragment # 0 - This will show FirstFragment
                return new WordListFragment();
            case 1: // Fragment # 0 - This will show FirstFragment different title
                return new VideoResultFragment();
            default:
                return null;
        }
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        return "Page " + position;
    }

}
