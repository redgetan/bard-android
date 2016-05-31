package com.roplabs.bard.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.roplabs.bard.R;
import com.roplabs.bard.ui.RepoListActivity;
import com.roplabs.bard.ui.TagView;
import com.roplabs.bard.ui.VideoPlayerActivity;

import java.util.List;

public class WordListAdapter extends
        RecyclerView.Adapter<WordListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<String> wordList;
    private Context context;

    // Pass in the contact array into the constructor
    public WordListAdapter(Context context, List<String> wordList) {
        this.wordList = wordList;
        this.context = context;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public WordListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        TagView tagView = new TagView(this.context, "");
        initTagView(tagView);

        // Return a new holder instance
        return new ViewHolder(this.context, tagView);
    }

    private void initTagView(TagView tagView){
        tagView.setTagBackgroundColor(Color.parseColor("#EEEEFF"));
        tagView.setTagBorderColor(Color.parseColor("#333333"));
        tagView.setTagTextColor(Color.parseColor("#333333"));
        tagView.setTagMaxLength(23);
        tagView.setTextDirection(View.TEXT_DIRECTION_LTR);
        tagView.setTypeface(Typeface.DEFAULT);
//        tagView.setBorderWidth(mTagBorderWidth);
        tagView.setBorderRadius(10.0f);
        tagView.setTextSize(30.0f);
        tagView.setHorizontalPadding(10);
        tagView.setVerticalPadding(10);
        tagView.setIsViewClickable(true);
        tagView.setBdDistance(5.5f);
//        tagView.setOnTagClickListener(mOnTagClickListener);
    }

    public List<String> getList() {
        return this.wordList;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(WordListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        String word = wordList.get(position);

        // Set item views based on the data model
        viewHolder.wordTag.setText(word);
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return wordList.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TagView wordTag;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            wordTag = (TagView) itemView;

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            String word = wordList.get(position);

            Toast.makeText(this.context,word,Toast.LENGTH_SHORT).show();
        }

    }
}
