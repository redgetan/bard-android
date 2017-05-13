package com.roplabs.bard.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Post;

import java.util.List;

public class ChannelFeedAdapter extends
        RecyclerView.Adapter<ChannelFeedAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Post> posts;
    private Context context;
    private View lastSelectedView;
    private int selectedPosition = -1;

    // Pass in the contact array into the constructor
    public ChannelFeedAdapter(Context context, List<Post> postList) {
        this.posts = postList;
        this.context = context;
    }

    public void swap(List<Post> posts){
        this.posts = posts;
        notifyDataSetChanged();
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public ChannelFeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.channel_feed_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ChannelFeedAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Post post = posts.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.channelFeedRepoTitle;
        textView.setText(buildPostText(post));

        ImageView thumbnail = viewHolder.channelFeedThumbnail;
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(context)
                .load(post.getThumbnailUrl())
                .placeholder(R.drawable.thumbnail_placeholder)
                .crossFade()
                .into(thumbnail);

        viewHolder.itemView.setSelected(selectedPosition == position);
    }

    private SpannableStringBuilder buildPostText(Post post) {
        String postUsername;
        if (post.getUsername().equals("Anonymous")) {
            postUsername = "";
        } else {
            postUsername = post.getUsername() + ":";
        }
        String postMessage  = post.getTitle();
        final SpannableStringBuilder sb = new SpannableStringBuilder(postUsername + " " + postMessage);

        // Span to set text color to some RGB value
        final ForegroundColorSpan fcs = new ForegroundColorSpan(R.color.purple);

        // Span to make text bold
        final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

        // Set the text color for first 4 characters
        sb.setSpan(fcs, 0, postUsername.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        // make them also bold
        sb.setSpan(bss, 0, postUsername.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return sb;
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return posts.size();
    }

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Post post);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView channelFeedRepoTitle;
        public ImageView channelFeedThumbnail;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            channelFeedRepoTitle = (TextView) itemView.findViewById(R.id.channel_feed_repo_title);
            channelFeedThumbnail = (ImageView) itemView.findViewById(R.id.channel_feed_thumbnail);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Post post = posts.get(position);

            setSelected(position);

            if (listener != null) {
                listener.onItemClick(v, position, post);
            }
        }

    }

    public void setSelected(int targetPosition) {
        notifyItemChanged(selectedPosition);
        selectedPosition = targetPosition;
        notifyItemChanged(selectedPosition);
    }

    public int getSelected() {
        return selectedPosition;
    }

}
