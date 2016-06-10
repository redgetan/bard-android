package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WordsLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.WordListAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class WordListFragment extends Fragment {
    // Store instance variables
    private RecyclerView recyclerView;
    private EditText findInPageInput;
    private Button findNextBtn;
    HashMap<String, ArrayList<Integer>> wordPositionsMap;
    private int currentWordPositionIndex;
    private WordListAdapter.ViewHolder lastViewHolder;
    private int currentScrollPosition;
    private String lastWord;

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

        findInPageInput = (EditText) view.findViewById(R.id.input_find_in_page);
        findNextBtn = (Button) view.findViewById(R.id.btn_find_next);
        lastWord = "";

        recyclerView = (RecyclerView) view.findViewById(R.id.current_word_list);
        recyclerView.setLayoutManager(new WordsLayoutManager(ClientApp.getContext()));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (currentScrollPosition != -1) {
                    WordListAdapter.ViewHolder viewHolder = (WordListAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(currentScrollPosition);
                    viewHolder.tagView.setBackgroundColor(Color.YELLOW);
                    lastViewHolder = viewHolder;
                    currentScrollPosition = -1;
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        listener.onWordListViewReady(recyclerView);

        return view;
    }

    public void initFindInPage(String[] availableWordList) {
        fillWordPositionsMap(availableWordList);
        initFindInPageListener();
        this.currentWordPositionIndex = 0;
    }

    public void fillWordPositionsMap(String[] words) {
        this.wordPositionsMap = new HashMap<String, ArrayList<Integer>>();

        ArrayList<Integer> positions;

        int i = 0;
        while (i < words.length) {
            String word = words[i].split(":")[0];

            if ((positions = wordPositionsMap.get(word)) != null) {
                positions.add(i);
            } else {
                positions = new ArrayList<Integer>();
                positions.add(i);
                wordPositionsMap.put(word, positions);
            }

            i++;
        }
    }


    public void initFindInPageListener() {
        findNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNextWord(findInPageInput.getText().toString());
            }
        });



        findInPageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                findNextWord(s.toString());
            }
        });
    }

    public void findNextWord(String word) {
        ArrayList<Integer> positions = wordPositionsMap.get(word);
        if (positions == null) return;

        if (!lastWord.equals(word)) {
            this.currentWordPositionIndex = 0;
        }

        int position = positions.get(this.currentWordPositionIndex);
        recyclerView.scrollToPosition(position);
        currentScrollPosition = position;

        if (lastViewHolder != null) lastViewHolder.tagView.setBackgroundColor(Color.TRANSPARENT);

        currentWordPositionIndex++;
        if (currentWordPositionIndex > positions.size() - 1) {
            currentWordPositionIndex = 0;
        }

        lastWord = word;
    }


}
