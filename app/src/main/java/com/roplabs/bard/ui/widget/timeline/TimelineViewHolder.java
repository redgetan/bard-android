package com.roplabs.bard.ui.widget.timeline;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import im.ene.toro.ToroAdapter;

public abstract class TimelineViewHolder extends ToroAdapter.ViewHolder {

    private static LayoutInflater inflater;

    public TimelineViewHolder(View itemView) {
        super(itemView);
    }

    @Override public void onAttachedToWindow() {

    }

    @Override public void onDetachedFromWindow() {

    }

    static ToroAdapter.ViewHolder createViewHolder(ViewGroup parent, int type) {
        if (inflater == null || inflater.getContext() != parent.getContext()) {
            inflater = LayoutInflater.from(parent.getContext());
        }

        final ToroAdapter.ViewHolder viewHolder;
        final View view;
        switch (type) {
            case TimelineAdapter.TYPE_VIDEO:
                view = inflater.inflate(VideoViewHolder.LAYOUT_RES, parent, false);
                viewHolder = new VideoViewHolder(view);
                break;
            default:
                view = inflater.inflate(VideoViewHolder.LAYOUT_RES, parent, false);
                viewHolder = new VideoViewHolder(view);
                break;
        }

        return viewHolder;
    }
}

