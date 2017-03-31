package com.roplabs.bard.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ChannelFeedFragment extends Fragment {

    public static ChannelFeedFragment newInstance() {
        Bundle args = new Bundle();
        ChannelFeedFragment fragment = new ChannelFeedFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
