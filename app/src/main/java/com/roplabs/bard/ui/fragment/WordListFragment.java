package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WordsLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.*;
import com.roplabs.bard.models.Segment;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.models.WordTagSelector;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WordListFragment extends Fragment {
    // Store instance variables
    private RecyclerView recyclerView;
    private ImageView findNextBtn;
    private ImageView findPrevBtn;
    private TextView word_tag_status;
    private TextView display_word_error;

    private WordTagSelector wordTagSelector;
    private VideoView previewTagView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;

    private OnWordListViewReadyListener listener;

    // Define the events that the fragment will use to communicate
    public interface OnWordListViewReadyListener  {
        // This can be any number of events to be sent to the activity
        public void onWordListViewReady(RecyclerView recyclerView);
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWordListViewReadyListener) {
            listener = (OnWordListViewReadyListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement WordListFragment.OnWordListViewReadyListener");
        }
    }


    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_word_list, container, false);

        display_word_error = (TextView) view.findViewById(R.id.display_word_error);
        word_tag_status = (TextView) view.findViewById(R.id.word_tag_status);
        previewTagView = (VideoView) view.findViewById(R.id.preview_tag_view);
        findNextBtn = (ImageView) view.findViewById(R.id.btn_find_next);
        findPrevBtn = (ImageView) view.findViewById(R.id.btn_find_prev);

        initVideoPlayer();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(InvalidWordEvent event) {
        display_word_error.setText(event.text);
    }

    @Subscribe
    public void onEvent(FindWordEvent event) {
        String wordTag = wordTagSelector.findNextWord(event.word);
        queryWordPreview(wordTag);
        EventBus.getDefault().post(new AddWordEvent(wordTag));
    }

    @Subscribe
    public void onEvent(PreviewWordEvent event) {
        String wordTag = event.word;
        wordTagSelector.setWordTag(wordTag);
        queryWordPreview(wordTag);
    }

    private void queryWordPreview(String wordTag) {
        if (wordTag.isEmpty()) return;

        try {
            BardClient.getQuery(wordTag, Setting.getCurrentIndexToken(ClientApp.getContext()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playPreview(Segment segment) {
        drawPagination();
        playVideo(segment.getSourceUrl());
    }

    private void drawPagination() {
        StringBuilder builder = new StringBuilder();

        builder.append(wordTagSelector.getCurrentWord());
        builder.append(" (");
        builder.append(wordTagSelector.getCurrentWordTagIndex() + 1);
        builder.append(" / ");
        builder.append(wordTagSelector.getCurrentWordTagCount());
        builder.append(" )");

        word_tag_status.setText(builder.toString());
    }

    private void initVideoPlayer() {
        previewTagView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                previewTagView.setBackgroundColor(Color.TRANSPARENT);
                isVideoReady = true;
                mediaPlayer = mp;
            }
        });

        previewTagView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // http://stackoverflow.com/a/14163267
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!isVideoReady) return false;

                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();

                    return false;
                } else {
                    return true;
                }

            }
        });
    }


    public void playVideo(String sourceUrl) {
        previewTagView.setVideoPath(sourceUrl);
        previewTagView.start();
    }


    public void initFindInPage(String[] availableWordList) {
        wordTagSelector = new WordTagSelector(availableWordList);
        initFindInPageListener();
    }

    public void initFindInPageListener() {
        findNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wordTag = wordTagSelector.findNextWord();
                queryWordPreview(wordTag);
                EventBus.getDefault().post(new ReplaceWordEvent(wordTag));
            }
        });

        findPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wordTag = wordTagSelector.findPrevWord();
                queryWordPreview(wordTag);
                EventBus.getDefault().post(new ReplaceWordEvent(wordTag));
            }
        });


    }

}
