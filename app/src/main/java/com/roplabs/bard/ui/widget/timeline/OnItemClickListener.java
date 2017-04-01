package com.roplabs.bard.ui.widget.timeline;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public interface OnItemClickListener {

    void onItemClick(RecyclerView.Adapter adapter, RecyclerView.ViewHolder viewHolder, View view,
                     int adapterPosition, long itemId);
}

