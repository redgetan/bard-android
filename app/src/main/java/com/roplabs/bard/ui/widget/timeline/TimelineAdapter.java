package com.roplabs.bard.ui.widget.timeline;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.roplabs.bard.models.Post;
import com.roplabs.bard.models.Repo;
import im.ene.toro.BaseAdapter;
import im.ene.toro.ToroAdapter;
import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends BaseAdapter<ToroAdapter.ViewHolder>
        implements OrderedPlayList {

    static final int TYPE_OGP = 1;
    static final int TYPE_PHOTO = 2;
    static final int TYPE_VIDEO = 3;


    private final List<TimelineItem> items;
    private Context context;

    public TimelineAdapter(Context context, List<Post> postList) {
        this.context = context;

        this.items = new ArrayList<TimelineItem>();
        for (int i = 0; i < postList.size(); i++) {
            items.add(new TimelineItem(context, postList.get(i)));
        }
    }

    @NonNull @Override protected TimelineItem getItem(int position) {
        return items.get(position);
    }

    ItemClickListener onItemClickListener;

    public void setOnItemClickListener(ItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ViewHolder viewHolder = TimelineViewHolder.createViewHolder(parent, viewType);
        if (viewHolder instanceof VideoViewHolder) {
            // TODO Click to Video
            viewHolder.setOnItemClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION
                            && onItemClickListener != null
                            && v == ((VideoViewHolder) viewHolder).getPlayerView()) {
                        onItemClickListener.onVideoClick(viewHolder, v,
                                (TimelineItem.VideoItem) getItem(position).getEmbedItem());
                    }
                }
            });
        }

        return viewHolder;
    }

    @Override public int getItemCount() {
        return this.items.size();
    }

    @Override public int firstVideoPosition() {
        int firstVideo = -1;
        for (int i = 0; i < items.size(); i++) {
            if (TimelineItem.VideoItem.class.getSimpleName()
                    .equals(getItem(i).getEmbedItem().getClassName())) {
                firstVideo = i;
                break;
            }
        }

        return firstVideo;
    }

    @Override public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override public int getItemViewType(int position) {
        String itemClassName = getItem(position).getEmbedItem().getClassName();
        return TimelineItem.VideoItem.class.getSimpleName().equals(itemClassName) ? TYPE_VIDEO
                : (TimelineItem.PhotoItem.class.getSimpleName().equals(itemClassName) ? TYPE_PHOTO
                : TYPE_OGP);
    }

    public static abstract class ItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(RecyclerView.Adapter adapter, RecyclerView.ViewHolder viewHolder,
                                View view, int adapterPosition, long itemId) {

        }

        protected abstract void onPhotoClick(RecyclerView.ViewHolder viewHolder, View view,
                                             TimelineItem.PhotoItem item);

        protected abstract void onVideoClick(RecyclerView.ViewHolder viewHolder, View view,
                                             TimelineItem.VideoItem item);
    }

}

