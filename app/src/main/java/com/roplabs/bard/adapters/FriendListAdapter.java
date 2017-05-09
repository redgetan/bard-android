package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Friend;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.widget.LetterAvatar;

import java.util.List;

public class FriendListAdapter extends
        RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Friend> friends;
    private Context context;

    // Pass in the contact array into the constructor
    public FriendListAdapter(Context context, List<Friend> friendList) {
        this.friends = friendList;
        this.context = context;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public FriendListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.user_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(FriendListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Friend user = friends.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.userUsername;
        textView.setText(user.getFriendname());

        TextView thumbnail = viewHolder.userThumbnail;
        thumbnail.setText(user.getFriendname().substring(0,1).toUpperCase());

        textView = viewHolder.userAction;
        textView.setVisibility(View.GONE);

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return friends.size();
    }

    private static OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Friend user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView userUsername;
        public TextView userThumbnail;
        public TextView userAction;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            userUsername = (TextView) itemView.findViewById(R.id.user_username);
            userThumbnail = (TextView) itemView.findViewById(R.id.user_thumbnail);
            userAction = (TextView) itemView.findViewById(R.id.user_action);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Friend user = friends.get(position);

            if (listener != null) {
                listener.onItemClick(v, position, user);
            }
        }

    }
}
