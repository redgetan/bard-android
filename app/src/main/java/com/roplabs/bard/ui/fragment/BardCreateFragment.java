package com.roplabs.bard.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.BardCreateFragmentPagerAdapter;

/**
 * Created by reg on 2017-04-17.
 */
public class BardCreateFragment extends Fragment {

    private ViewPager viewPager;

    public static BardCreateFragment newInstance() {
        Bundle args = new Bundle();
        BardCreateFragment fragment = new BardCreateFragment();
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
        View view = inflater.inflate(R.layout.fragment_bard_create, container, false);

        initPager(view);

        return view;
    }


    private void initPager(View view) {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) view.findViewById(R.id.bard_create_pager);
        viewPager.setAdapter(new BardCreateFragmentPagerAdapter(getActivity().getSupportFragmentManager(),
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


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.bard_create_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

}
