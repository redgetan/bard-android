package com.roplabs.bard.adapters;

import android.content.Context;
        import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ImageView;
        import android.widget.TextView;
        import com.bumptech.glide.Glide;
        import com.roplabs.bard.R;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.ui.widget.LetterAvatar;
import com.roplabs.bard.util.Helper;

import java.util.List;

public class ChannelListAdapter extends
        RecyclerView.Adapter<ChannelListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Channel> channels;
    private Context context;

    // Pass in the contact array into the constructor
    public ChannelListAdapter(Context context, List<Channel> channeList) {
        this.channels = channeList;
        this.context = context;
    }

    public void swap(List<Channel> channels){
        this.channels = channels;
        notifyDataSetChanged();
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public ChannelListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.channel_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ChannelListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Channel channel = channels.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.channelTitleView;
        if (channel.getMode().equals("pair")) {
            textView.setText(channel.getReceiver());
        } else {
            textView.setText(channel.getName());
        }

        textView = viewHolder.channelDescriptionView;
        if (!channel.getLastMessage().isEmpty()) {
            textView.setText(Helper.truncate(channel.getLastMessage(), 30));
        } else {
            textView.setText("Tap to chat"); // necessary in order to reset what was previously set in that row (as its recycled)

        }

        textView = viewHolder.channelLastUpdatedAt;
        if (channel.getUpdatedAt() != null) {
            textView.setText(Helper.formatDate(channel.getUpdatedAt()));
        }

//        ImageView thumbnail = viewHolder.channelThumbnail;
//        int[] colors = new int[] { R.color.md_red_300, R.color.md_blue_300, R.color.md_amber_200, R.color.md_green_200, R.color.md_purple_100 };
//        int color = colors[position % colors.length];
//        LetterAvatar avatar = new LetterAvatar(context, ContextCompat.getColor(this.context, color), channel.getName().toUpperCase().substring(0,1), 20);
//        thumbnail.setImageDrawable(avatar);
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return channels.size();
    }

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Channel channel);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView channelTitleView;
        public TextView channelDescriptionView;
        public TextView channelLastUpdatedAt;
        public ImageView channelThumbnail;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            channelTitleView = (TextView) itemView.findViewById(R.id.channel_title);
            channelDescriptionView = (TextView) itemView.findViewById(R.id.channel_description);
            channelThumbnail = (ImageView) itemView.findViewById(R.id.channel_thumbnail);
            channelLastUpdatedAt = (TextView) itemView.findViewById(R.id.channel_last_updated_at);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Channel channel = channels.get(position);

            if (listener != null) {
                listener.onItemClick(v, position, channel);
            }
        }

    }
}
