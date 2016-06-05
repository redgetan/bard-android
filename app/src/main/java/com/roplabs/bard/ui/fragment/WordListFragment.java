package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WordsLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;

public class WordListFragment extends Fragment {
    // Store instance variables
    private RecyclerView recyclerView;

    private OnWordListViewReadyListener listener;

    // Define the events that the fragment will use to communicate
    public interface OnWordListViewReadyListener  {
        // This can be any number of events to be sent to the activity
        public void onWordListViewReady(RecyclerView recyclerView);
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWordListViewReadyListener) {
            listener = (OnWordListViewReadyListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement WordListFragment.OnWordListViewReadyListener");
        }
    }


    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_word_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.current_word_list);
        recyclerView.setLayoutManager(new WordsLayoutManager(ClientApp.getContext()));

        listener.onWordListViewReady(recyclerView);

        return view;
    }
}
