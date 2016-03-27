package com.sandbox.myfirstapp.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.Repo;
import com.sandbox.myfirstapp.app.ui.RepoListActivity;
import com.sandbox.myfirstapp.app.ui.VideoPlayerActivity;

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
        textView.setText(repo.getWordList());

        Button button = viewHolder.viewButton;
        button.setText("view");

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return repos.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView repoTitleView;
        public Button viewButton;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            repoTitleView = (TextView) itemView.findViewById(R.id.repo_title);
            viewButton = (Button) itemView.findViewById(R.id.view_button);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Repo repo = repos.get(position);

            Intent intent = new Intent(this.context, VideoPlayerActivity.class);
            intent.putExtra(RepoListActivity.VIDEO_LOCATION_MESSAGE, repo.getFilePath());
            this.context.startActivity(intent);
        }

    }
}
