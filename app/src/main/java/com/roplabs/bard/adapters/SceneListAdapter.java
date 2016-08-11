package com.roplabs.bard.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.Setting;

import java.util.List;

public class SceneListAdapter extends RecyclerView.Adapter<SceneListAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Scene> sceneList;
    private Context context;
    private View selectedView;

    // Pass in the contact array into the constructor
    public SceneListAdapter(Context context, List<Scene> sceneList) {
        this.context = context;
        this.sceneList = sceneList;
    }

    public void swap(List<Scene> scenes){
        sceneList = scenes;
        notifyDataSetChanged();
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public SceneListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.scene_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(this.context, contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(SceneListAdapter.ViewHolder viewHolder, int position) {
        if (position == 0) {
            TextView textView = viewHolder.sceneNameView;
            textView.setText("All");

            ImageView thumbnail = viewHolder.sceneThumbnail;
            Bitmap bitmap = BitmapFactory.decodeResource(ClientApp.getContext().getResources(),
                                                         R.drawable.thumbnail_placeholder);
            thumbnail.setImageBitmap(bitmap);
        } else {

            // Get the data model based on position
            Scene scene = sceneList.get(position - 1);

            // Set item views based on the data model
            TextView textView = viewHolder.sceneNameView;
            textView.setText(scene.getName());

            ImageView thumbnail = viewHolder.sceneThumbnail;
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(context)
                    .load(scene.getThumbnailUrl())
                    .placeholder(R.drawable.thumbnail_placeholder)
                    .crossFade()
                    .into(thumbnail);
        }


    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return sceneList.size() + 1;
    }

    private static OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position, Scene scene);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView sceneNameView;
        public ImageView sceneThumbnail;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);
            sceneNameView = (TextView) itemView.findViewById(R.id.scene_title);
            sceneThumbnail = (ImageView) itemView.findViewById(R.id.scene_thumbnail);

            this.context = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getLayoutPosition();
            Scene scene = null;

            // position 0 is reserved for "All" scene, anything greater must be adjusted to 0-index
            if (position > 0) {
                scene = sceneList.get(position - 1);
            }

            if (selectedView != null) {
                selectedView.setSelected(false);
            }

            selectedView = v;

            selectedView.setSelected(true);
            if (listener != null) {
                listener.onItemClick(v, position, scene);
            }
        }
    }
}
