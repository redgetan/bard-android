package com.roplabs.bard.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Repo;
import com.roplabs.bard.ui.activity.RepoListActivity;
import com.roplabs.bard.ui.activity.VideoPlayerActivity;

import java.util.List;

public class RepoListAdapter extends
        RecyclerView.Adapter<RepoListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Repo> repos;
    private Context context;

    // Pass in the contact array into the constructor
    public RepoListAdapter(Context context, List<Repo> repoList) {
        this.repos = repoList;
        this.context = context;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public RepoListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.repo_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(RepoListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Repo repo = repos.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.repoTitleView;
        textView.setText(repo.title());

        ImageView thumbnail = viewHolder.repoThumbnail;
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (repo.getFilePath().isEmpty()) {
            Glide.with(context)
                    .load(repo.getThumbnailUrl())
                    .crossFade()
                    .into(thumbnail);
        } else {
            Glide.with(context)
                    .load(repo.getFilePath())
                    .crossFade()
                    .into(thumbnail);
        }
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return repos.size();
    }

    private static OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Repo repo);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView repoTitleView;
        public ImageView repoThumbnail;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            repoTitleView = (TextView) itemView.findViewById(R.id.repo_title);
            repoThumbnail = (ImageView) itemView.findViewById(R.id.repo_thumbnail);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Repo repo = repos.get(position);

            if (listener != null) {
                listener.onItemClick(v, position, repo);
            }
        }

    }
}
