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

public class WordListFragment extends Fragment implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
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

    private OnWordTagChanged wordTagChangedListener;

    public interface OnWordTagChanged {
        void onWordTagChanged(WordTag wordTag, int delayInMilliseconds);
    }

    private OnPreviewPlayerPreparedListener previewPlayerPreparedListener;

    public interface OnPreviewPlayerPreparedListener  {
        void onPreviewPlayerPrepared();
    }

    public void setOnPreviewPlayerPreparedListener(OnPreviewPlayerPreparedListener listener) {
        this.previewPlayerPreparedListener = listener;
    }

    public void setOnWordTagChangedListener(OnWordTagChanged listener) {
        this.wordTagChangedListener = listener;
    }

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
        BardLogger.trace("mediaplayer onPrepared");
        isVideoReady = true;

        if (this.previewPlayerPreparedListener != null) {
            this.previewPlayerPreparedListener.onPreviewPlayerPrepared();
        }

        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

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

        previewContainer = (FrameLayout) view.findViewById(R.id.preview_container);
        display_word_error = (TextView) view.findViewById(R.id.display_word_error);
        word_tag_status = (TextView) view.findViewById(R.id.word_tag_status);
        previewTagView = (TextureView) view.findViewById(R.id.preview_tag_view);
        previewOverlay = view.findViewById(R.id.preview_video_overlay);
        wordTagPlayHandler = new Handler();

        initVideoPlayer();

        listener.onWordListFragmentReady();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(InvalidWordEvent event) {
        if (event.text.isEmpty()) {
            display_word_error.setVisibility(View.GONE);
        } else {
            display_word_error.setVisibility(View.VISIBLE);
        }

        display_word_error.setText(event.text);
    }

    public void setWordTag(WordTag wordTag) {
        BardLogger.trace("setWordTag: " + wordTag.toString());
        wordTagSelector.setWordTag(wordTag);
        onWordTagChanged(wordTag);
    }

    public void setWordTagWithDelay(WordTag wordTag, int delayInMilliSeconds) {
        BardLogger.trace("setWordTag with delay: " + wordTag.toString());
        wordTagSelector.setWordTag(wordTag);
        onWordTagChanged(wordTag, delayInMilliSeconds);
    }

    public void onWordTagChanged(final WordTag wordTag, final int delayInMilliSeconds) {
        if (wordTag == null) return;

        drawPagination();

        if (wordTagChangedListener != null) {
            wordTagChangedListener.onWordTagChanged(wordTag, delayInMilliSeconds);
        }
    }

    public void onWordTagChanged(final WordTag wordTag) {
        if (wordTag == null) return;
        drawPagination();
        wordTagChangedListener.onWordTagChanged(wordTag, 0);
    }

    public void playPreview(String url) {
        if (previewOverlay.isShown()) previewOverlay.setVisibility(View.GONE);
        playVideo(url);
    }

    private void drawPagination() {
        StringBuilder builder = new StringBuilder();

//        builder.append(wordTagSelector.getCurrentWord());
//        builder.append(" (");
        builder.append(wordTagSelector.getCurrentWordTagIndex() + 1);
        builder.append(" of ");
        builder.append(wordTagSelector.getCurrentWordTagCount());
//        builder.append(" )");

        word_tag_status.setText(builder.toString());
    }

    public void fixVideoAspectRatio() {
        // keep aspect ratio to 16/9
        Matrix txform = new Matrix();
        previewTagView.getTransform(txform);
        int viewHeight = previewTagView.getHeight();
        int viewWidth = previewTagView.getWidth();
        int newHeight = viewHeight;
        int newWidth = (int) (1.9 * viewHeight);
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        previewTagView.setTransform(txform);

    }

    private void initVideoPlayer() {
        // video
        previewTagView.setOpaque(false);
        previewTagView.setSurfaceTextureListener(this);

        previewTagView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                if (wordTagSelector == null) return;
                WordTag targetWordTag = wordTagSelector.findNextWord();
                if (targetWordTag != null) onWordTagChanged(targetWordTag, 500);
            }

            @Override
            public void onSwipeRight() {
                if (wordTagSelector == null) return;
                WordTag targetWordTag = wordTagSelector.findPrevWord();
                if (targetWordTag != null) onWordTagChanged(targetWordTag, 500);
            }

            @Override
            public void onTouchUp() {
                if (!isVideoReady) return;

                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });


        ViewTreeObserver vto = previewTagView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {

                ViewTreeObserver obs = previewTagView.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });

    }


    public void playVideo(String sourceUrl) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(sourceUrl);
            mediaPlayer.setSurface(previewSurface);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
    }

    public void resetVideo() {
        previewOverlay.setVisibility(View.VISIBLE);
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
        BardLogger.log("video=" + videoWidth + "x" + videoHeight +
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


    public void initWordTagSelector(List<String> availableWordList) {
        wordTagSelector = new WordTagSelector(availableWordList);
    }

    public WordTagSelector getWordTagSelector() {
        return wordTagSelector;
    }

}
