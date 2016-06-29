package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WordsLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.crashlytics.android.Crashlytics;
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

public class WordListFragment extends Fragment implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    // Store instance variables
    private RecyclerView recyclerView;
    private ImageView findNextBtn;
    private ImageView findPrevBtn;
    private TextView word_tag_status;
    private TextView display_word_error;

    private WordTagSelector wordTagSelector;
    private TextureView previewTagView;
    private MediaPlayer mediaPlayer;
    private boolean isVideoReady = false;
    private Surface previewSurface;
    private View previewOverlay;

    private OnReadyListener listener;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        previewSurface = new Surface(surface);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setSurface(previewSurface);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {


    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        previewTagView.setBackgroundColor(Color.TRANSPARENT);
        isVideoReady = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        listener.onVideoThumbnailChanged(previewTagView.getBitmap(100,100));
    }

    // Define the events that the fragment will use to communicate
    public interface OnReadyListener  {
        // This can be any number of events to be sent to the activity
        public void onWordListFragmentReady();
        public void onVideoThumbnailChanged(Bitmap bitmap);
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

        display_word_error = (TextView) view.findViewById(R.id.display_word_error);
        word_tag_status = (TextView) view.findViewById(R.id.word_tag_status);
        previewTagView = (TextureView) view.findViewById(R.id.preview_tag_view);
        findNextBtn = (ImageView) view.findViewById(R.id.btn_find_next);
        findPrevBtn = (ImageView) view.findViewById(R.id.btn_find_prev);
        previewOverlay = view.findViewById(R.id.preview_video_overlay);

        initVideoPlayer();

        listener.onWordListFragmentReady();

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
    public void onEvent(PreviewWordEvent event) {
        String wordTagString = event.wordTagString;
        wordTagSelector.setWordTag(wordTagString);
        queryWordPreview(wordTagString);
    }

    public void queryWordPreview(String wordTagString) {
        if (wordTagString.isEmpty()) return;

        try {
            BardClient.getQuery(wordTagString, Setting.getCurrentIndexToken(ClientApp.getContext()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playPreview(Segment segment) {
        if (previewOverlay.isShown()) previewOverlay.setVisibility(View.GONE);
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
        previewTagView.setOpaque(false);
        previewTagView.setSurfaceTextureListener(this);


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
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(sourceUrl);
            mediaPlayer.setSurface(previewSurface);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }


    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = previewTagView.getWidth();
        int viewHeight = previewTagView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v("Mimic", "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        previewTagView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        previewTagView.setTransform(txform);
    }


    public void initFindInPage(String[] availableWordList) {
        wordTagSelector = new WordTagSelector(availableWordList);
        initFindInPageListener();
    }

    public WordTagSelector getWordTagSelector() {
        return wordTagSelector;
    }

    public void initFindInPageListener() {
        findNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wordTagString = wordTagSelector.findNextWord();
                queryWordPreview(wordTagString);
                EventBus.getDefault().post(new ReplaceWordEvent(wordTagString));
            }
        });

        findPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wordTagString = wordTagSelector.findPrevWord();
                queryWordPreview(wordTagString);
                EventBus.getDefault().post(new ReplaceWordEvent(wordTagString));
            }
        });


    }

}
