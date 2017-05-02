package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.roplabs.bard.R;
import com.roplabs.bard.ui.fragment.SceneSelectFragment;
import com.roplabs.bard.util.Helper;

import java.util.Arrays;

/**
 * Created by reg on 2017-04-18.
 */
public class MainSceneFragmentPagerAdapter extends FragmentPagerAdapter {

    private static String tabTitles[] = new String[]{
            Helper.UPLOADS_SCENE_TYPE,
            Helper.FAVORITES_SCENE_TYPE,
            Helper.ONLINE_LIBRARY,
            Helper.POLITICS_SCENE_TYPE,
            Helper.VIDEO_GAMES_SCENE_TYPE,
            Helper.YOUTUBER_SCENE_TYPE,
            Helper.CARTOON_SCENE_TYPE,
            Helper.INTERVIEW_SCENE_TYPE,
            Helper.TELEVISION_SCENE_TYPE,
            Helper.MOVIES_SCENE_TYPE,
            Helper.COMEDY_SCENE_TYPE,
            Helper.PUPPETS_SCENE_TYPE,
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
        return SceneSelectFragment.newInstance(sceneType);
    }

    public int getIcon(int index) {
        String tabTitle = tabTitles[index];
        if (tabTitle.equals(Helper.FAVORITES_SCENE_TYPE)) {
            return R.drawable.icon_bookmark_ribbon;
        } else if (tabTitle.equals(Helper.UPLOADS_SCENE_TYPE)) {
            return R.drawable.icon_uploads;
        } else if (tabTitle.equals(Helper.ONLINE_LIBRARY)) {
            return R.drawable.icon_general_circle;
        } else if (tabTitle.equals(Helper.POLITICS_SCENE_TYPE)) {
            return R.drawable.icon_politics;
        } else if (tabTitle.equals(Helper.VIDEO_GAMES_SCENE_TYPE)) {
            return R.drawable.icon_joystick;
        } else if (tabTitle.equals(Helper.YOUTUBER_SCENE_TYPE)) {
            return R.drawable.icon_youtuber;
        } else if (tabTitle.equals(Helper.CARTOON_SCENE_TYPE)) {
            return R.drawable.icon_cartoon;
        } else if (tabTitle.equals(Helper.INTERVIEW_SCENE_TYPE)) {
            return R.drawable.icon_interview;
        } else if (tabTitle.equals(Helper.TELEVISION_SCENE_TYPE)) {
            return R.drawable.icon_television;
        } else if (tabTitle.equals(Helper.MOVIES_SCENE_TYPE)) {
            return R.drawable.icon_movie;
        } else if (tabTitle.equals(Helper.COMEDY_SCENE_TYPE)) {
            return R.drawable.icon_comedy;
        } else if (tabTitle.equals(Helper.PUPPETS_SCENE_TYPE)) {
            return R.drawable.icon_puppet;
        }

        return R.drawable.icon_general;
    }

    private static int getIndexOfTabTitle(String tabTitle) {
        return Arrays.asList(tabTitles).indexOf(tabTitle);
    }

    public static int getBookmarksFragmentPosition() {
        return getIndexOfTabTitle(Helper.FAVORITES_SCENE_TYPE);
    }

    public static int getUploadsFragmentPosition() {
        return getIndexOfTabTitle(Helper.UPLOADS_SCENE_TYPE);
    }

    public static int getOnlineLibraryFragmentPosition() {
        return getIndexOfTabTitle(Helper.ONLINE_LIBRARY);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if (tabTitles[position].equals(Helper.ONLINE_LIBRARY)) {
            return "All";
        } else if (tabTitles[position].equals(Helper.FAVORITES_SCENE_TYPE)) {
            return "Bookmarks";
        } else {
            return capitalize(tabTitles[position]).replaceAll("_"," ");
        }
    }

    private String capitalize(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
