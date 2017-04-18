package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.util.Helper;

import java.util.Arrays;

/**
 * Created by reg on 2017-04-18.
 */
public class MainSceneFragmentPagerAdapter extends FragmentPagerAdapter {

    private static String tabTitles[] = new String[]{
            Helper.ONLINE_LIBRARY,
            Helper.FAVORITES_SCENE_TYPE,
    };
    private Context context;
    private String channelToken;

    public MainSceneFragmentPagerAdapter(FragmentManager fm, Context context) {
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
        if (sceneType.equals(Helper.ONLINE_LIBRARY)) {
            return SceneSelectFragment.newInstance(sceneType);
        } else {
            return SceneSelectFragment.newInstance(sceneType);
        }
    }

    public static int getBookmarksFragmentPosition() {
        return Arrays.asList(tabTitles).indexOf(Helper.FAVORITES_SCENE_TYPE);
    }

    public static int getOnlineLibraryFragmentPosition() {
        return Arrays.asList(tabTitles).indexOf(Helper.ONLINE_LIBRARY);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.ONLINE_LIBRARY)) {
            return "All";
        } else {
            return "Bookmarks";
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
