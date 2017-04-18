package com.roplabs.bard.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.MainSceneFragmentPagerAdapter;
import org.w3c.dom.Text;

/**
 * Created by reg on 2017-04-18.
 */
public class MainSceneSelectFragment extends Fragment{

    private ViewPager viewPager;
    private LinearLayout userBookmarks;

    public static MainSceneSelectFragment newInstance() {
        Bundle args = new Bundle();
        MainSceneSelectFragment fragment = new MainSceneSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_scene_select, container, false);

        initPager(view);

        return view;
    }


    private void initPager(View view) {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) view.findViewById(R.id.main_scene_select_pager);
        viewPager.setAdapter(new MainSceneFragmentPagerAdapter(getActivity().getSupportFragmentManager(),
                getActivity()));
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


        userBookmarks = (LinearLayout) view.findViewById(R.id.my_videos);
        final TextView label = (TextView) view.findViewById(R.id.my_videos_label);
        final ImageView leftArrow = (ImageView) view.findViewById(R.id.left_navigation_icon);
        final ImageView rightArrow = (ImageView) view.findViewById(R.id.right_navigation_icon);

        userBookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() == MainSceneFragmentPagerAdapter.getOnlineLibraryFragmentPosition()) {
                    viewPager.setCurrentItem(MainSceneFragmentPagerAdapter.getBookmarksFragmentPosition());
                    label.setText("Back to All");

                    Bitmap allSceneBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_keyboard_arrow_left_black_24dp);
                    leftArrow.setImageBitmap(allSceneBitmap);
                    rightArrow.setVisibility(View.GONE);
                } else {
                    viewPager.setCurrentItem(MainSceneFragmentPagerAdapter.getOnlineLibraryFragmentPosition());
                    label.setText("Bookmarks");

                    Bitmap allSceneBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_bookmark_border_black_24dp);
                    leftArrow.setImageBitmap(allSceneBitmap);
                    rightArrow.setVisibility(View.VISIBLE);
                }
            }
        });

    }
}
