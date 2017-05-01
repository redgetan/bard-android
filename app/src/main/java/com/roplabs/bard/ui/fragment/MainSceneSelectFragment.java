package com.roplabs.bard.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
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
import com.roplabs.bard.ui.widget.NonSwipingViewPager;
import org.w3c.dom.Text;

/**
 * Created by reg on 2017-04-18.
 */
public class MainSceneSelectFragment extends Fragment{

    private NonSwipingViewPager viewPager;
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
        viewPager = (NonSwipingViewPager) view.findViewById(R.id.main_scene_select_pager);
        viewPager.setPagingEnabled(false);
        MainSceneFragmentPagerAdapter mainSceneSelectAdapter = new MainSceneFragmentPagerAdapter(getActivity().getSupportFragmentManager(),
                getActivity());
        viewPager.setAdapter(mainSceneSelectAdapter);
        viewPager.setOffscreenPageLimit(mainSceneSelectAdapter.getCount());
        viewPager.setCurrentItem(2);
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

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.main_scene_select_tab);
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(mainSceneSelectAdapter.getIcon(i));
        }


//        userBookmarks = (LinearLayout) view.findViewById(R.id.my_videos);
//        TextView label = (TextView) view.findViewById(R.id.my_videos_label);
//        ImageView leftArrow = (ImageView) view.findViewById(R.id.my_videos_left_arrow);
//        ImageView rightArrow = (ImageView) view.findViewById(R.id.my_videos_right_arrow);
//        initCategory(userBookmarks, R.drawable.ic_bookmark_border_black_24dp, label, leftArrow, rightArrow);
//
//        LinearLayout userUploads = (LinearLayout) view.findViewById(R.id.my_uploads);
//        label = (TextView) view.findViewById(R.id.my_videos_label);
//        leftArrow = (ImageView) view.findViewById(R.id.my_videos_left_arrow);
//        rightArrow = (ImageView) view.findViewById(R.id.my_videos_right_arrow);
//        initCategory(userUploads, R.drawable.ic_cloud_black_24dp, label, leftArrow, rightArrow);

    }

    private void initCategory(View view, final int drawableId, final TextView label, final ImageView leftArrow, final ImageView rightArrow) {
        view.setOnClickListener(new View.OnClickListener() {
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

                    Bitmap allSceneBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
                    leftArrow.setImageBitmap(allSceneBitmap);
                    rightArrow.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
