package com.roplabs.bard.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.events.TagClickEvent;
import com.roplabs.bard.models.WordTag;
import org.greenrobot.eventbus.EventBus;
import org.w3c.dom.Text;

import java.util.List;

public class WordListAdapter extends
        RecyclerView.Adapter<WordListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<String> wordList;
    private Context context;
    private boolean isWordTagged;

    // Pass in the contact array into the constructor
    public WordListAdapter(Context context, List<String> wordList) {
        this.wordList = wordList;
        this.context = context;
    }

    public void swap(List<String> wordTagList){
        wordList = wordTagList;
        notifyDataSetChanged();
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public WordListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View itemView = inflater.inflate(R.layout.word_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, itemView);
    }

    public List<String> getList() {
        return this.wordList;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(WordListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        String word = wordList.get(position);

        if (this.isWordTagged) {
            String[] wordTag = word.split(":");
            word = wordTag[0];
        }

        // Set item views based on the data model
        viewHolder.tagView.setText(word);
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return wordList.size();
    }

    public void setIsWordTagged(boolean isWordTagged) {
        this.isWordTagged = isWordTagged;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView tagView;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            tagView = (TextView) itemView;

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            String wordTagString = wordList.get(position);
            WordTag wordTag = new WordTag(wordTagString);

            EventBus.getDefault().post(new TagClickEvent(wordTag));
        }

    }
}
