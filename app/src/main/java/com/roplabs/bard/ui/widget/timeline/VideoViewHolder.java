package com.roplabs.bard.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.roplabs.bard.R;
import im.ene.toro.exoplayer2.*;

import java.io.File;

public class VideoViewHolder extends ExoPlayerViewHolder {

    static final int LAYOUT_RES = R.layout.channel_feed_item;

    private TimelineItem.VideoItem videoItem;
    private ImageView mThumbnail;
    private TextView mInfo;
    private MediaSource mediaSource;

    public VideoViewHolder(View itemView) {
        super(itemView);
        mThumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
        mInfo = (TextView) itemView.findViewById(R.id.info);
    }

    @Override protected ExoPlayerView findVideoView(View itemView) {
        return (ExoPlayerView) itemView.findViewById(R.id.video);
    }

    @Override
    protected MediaSource getMediaSource() {
        return this.mediaSource;
    }

    @Override protected void onBind(RecyclerView.Adapter adapter, @Nullable Object object) {
        if (!(object instanceof TimelineItem)
                || !(((TimelineItem) object).getEmbedItem() instanceof TimelineItem.VideoItem)) {
            throw new IllegalArgumentException("Only VideoItem is accepted");
        }

        this.videoItem = (TimelineItem.VideoItem) ((TimelineItem) object).getEmbedItem();
        String userAgent = com.google.android.exoplayer2.util.Util.getUserAgent(itemView.getContext(), "Toro-sample");
//        Uri uri = Uri.parse(this.videoItem.getVideoUrl());
        Uri uri = Uri.fromFile(new File("/storage/emulated/0/Movies/Bard/1487885490.mp4"));
                DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(itemView.getContext(),
                        userAgent);
        this.mediaSource = ExoPlayerHelper.buildMediaSource(
                                itemView.getContext(),
                                uri,
                                dataSourceFactory,
                                itemView.getHandler(),
                null);
    }

    @Override public void setOnItemClickListener(View.OnClickListener listener) {
        super.setOnItemClickListener(listener);
        mInfo.setOnClickListener(listener);
        this.playerView.setOnClickListener(listener);
    }

    @Nullable @Override public String getMediaId() {
        return Util.genVideoId(this.videoItem.getVideoUrl(), getAdapterPosition());
    }

    @Override public void onVideoPreparing() {
        super.onVideoPreparing();
        mInfo.setText("Preparing");
    }

    @Override public void onVideoPrepared() {
        super.onVideoPrepared();
        mInfo.setText("Prepared");
    }

    @Override public void onViewHolderBound() {
        super.onViewHolderBound();

        mThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(itemView.getContext())
                .load("https://i.ytimg.com/vi/mEoN_Xe-B-s/hqdefault.jpg")
                .placeholder(R.drawable.thumbnail_placeholder)
                .crossFade()
                .into(mThumbnail);

        mInfo.setText("Bound");
    }

    @Override public void onPlaybackStarted() {
        mThumbnail.animate().alpha(0.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                VideoViewHolder.super.onPlaybackStarted();
            }
        }).start();
        mInfo.setText("Started");
    }

    @Override public void onPlaybackPaused() {
        mThumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                VideoViewHolder.super.onPlaybackPaused();
            }
        }).start();
        mInfo.setText("Paused");
    }

    @Override public void onPlaybackCompleted() {
        mThumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                VideoViewHolder.super.onPlaybackCompleted();
            }
        }).start();
        mInfo.setText("Completed");
    }

    @Override public boolean onPlaybackError(Exception error) {
        mThumbnail.animate().alpha(1.f).setDuration(0).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                // Immediately finish the animation.
            }
        }).start();
        mInfo.setText("Error: videoId = " + getMediaId());
        return super.onPlaybackError(error);
    }
}

