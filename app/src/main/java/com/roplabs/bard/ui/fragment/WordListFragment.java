package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.*;
import com.roplabs.bard.models.Segment;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.WordTag;
import com.roplabs.bard.models.WordTagSelector;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.CrashReporter;
import com.roplabs.bard.util.OnSwipeTouchListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class WordListFragment extends Fragment {
    // Store instance variables
    private RecyclerView recyclerView;
    private ImageView findNextBtn;
    private ImageView findPrevBtn;
    private TextView word_tag_status;
    private TextView display_word_error;
    private FrameLayout previewContainer;

    private WordTagSelector wordTagSelector;
    private TextureView previewTagView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;
    private Surface previewSurface;
    private View previewOverlay;
    private Runnable fetchWordTagSegmentUrl;
    private Handler wordTagPlayHandler;

    private OnReadyListener listener;

    // Define the events that the fragment will use to communicate
    public interface OnReadyListener  {
        // This can be any number of events to be sent to the activity
        public void onWordListFragmentReady();
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnReadyListener) {
            listener = (OnReadyListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement WordListFragment.OnReadyListener");
        }
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_word_list, container, false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


}
