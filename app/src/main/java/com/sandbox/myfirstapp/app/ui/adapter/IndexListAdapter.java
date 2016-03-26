package com.sandbox.myfirstapp.app.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.sandbox.myfirstapp.app.R;
import com.sandbox.myfirstapp.app.models.Index;

import java.util.List;

public class IndexListAdapter extends
        RecyclerView.Adapter<IndexListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Index> indexList;

    // Pass in the contact array into the constructor
    public IndexListAdapter(List<Index> indexList) {
        this.indexList = indexList;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public IndexListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.index_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(IndexListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Index index = indexList.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.indexNameView;
        textView.setText(index.getName());

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return indexList.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView indexNameView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            indexNameView = (TextView) itemView.findViewById(R.id.index_name);
        }

    }
}
