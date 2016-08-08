package com.roplabs.bard.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Setting;

import java.util.List;

public class CharacterListAdapter extends
        RecyclerView.Adapter<CharacterListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Character> characterList;
    private Context context;
    private View selectedView;

    // Pass in the contact array into the constructor
    public CharacterListAdapter(Context context, List<Character> characterList) {
        this.context = context;
        this.characterList = characterList;
    }

    public void swap(List<Character> characters){
        characterList = characters;
        notifyDataSetChanged();
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public CharacterListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.index_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(CharacterListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Character character = characterList.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.indexNameView;
        textView.setText(character.getName());

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return characterList.size();
    }

    private static OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Character character);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView indexNameView;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);
            indexNameView = (TextView) itemView.findViewById(R.id.index_name);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Character character = characterList.get(position);

            if (selectedView != null) {
                selectedView.setSelected(false);
            }

            selectedView = v;

            selectedView.setSelected(true);
            if (listener != null) {
                listener.onItemClick(v, position, character);
            }
        }
    }
}
