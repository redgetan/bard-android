package com.roplabs.bard.ui.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.FindWordEvent;
import com.roplabs.bard.events.PreviewWordEvent;
import com.roplabs.bard.models.Setting;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class WordsAutoCompleteTextView extends EditText implements Filterable, Filter.FilterListener {
    MultiAutoCompleteTextView.Tokenizer mTokenizer;
    RecyclerView recyclerView;
    private Filter mFilter;
    private Trie<String,String> mWordTrie;
    private boolean isAutocompleteEnabled;
    private boolean isFindInPage;

    public WordsAutoCompleteTextView(Context context) {
        super(context);
        initWordsAutoComplete();
    }

    public WordsAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWordsAutoComplete();
    }

    public WordsAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWordsAutoComplete();
    }

    public void initWordsAutoComplete() {
        addTextChangedListener(new MyWatcher());
        this.isAutocompleteEnabled = true;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTokenizer != null) {
                    Editable text = getText();
                    int startPos = mTokenizer.findTokenStart(text,getSelectionEnd());
                    int endPos = mTokenizer.findTokenEnd(text,getSelectionEnd());

                    String word = text.subSequence(startPos, endPos).toString();

                    boolean isMiddleOfWordTag = !word.isEmpty() && word.contains(":");
                    if (isMiddleOfWordTag) {
                        String wordTag = word;
                        EventBus.getDefault().post(new PreviewWordEvent(wordTag));
                    }

                }
            }
        });
    }

    @Override
    public void onFilterComplete(int count) {

    }

    private class MyWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            doAfterTextChanged();
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String character = getText().subSequence(start,getSelectionEnd()).toString();

            if (character.equals(" ")) {
                int lastWordStartPos = mTokenizer.findTokenStart(getText(),start);
                int lastWordEndPos = mTokenizer.findTokenEnd(getText(),start);
                String lastWord = getText().subSequence(lastWordStartPos, lastWordEndPos).toString();
                if (!lastWord.isEmpty()) {
                    EventBus.getDefault().post(new FindWordEvent(lastWord));
                }
            }

        }
    }

    void doAfterTextChanged() {
        if (this.isAutocompleteEnabled) {
            performFiltering(getText(), 0);
        }
    }

    protected void performFiltering(CharSequence text, int keyCode) {
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(text, end);

        performFiltering(text, start, end, keyCode);
    }

    protected void performFiltering(CharSequence text, int start, int end,
                                    int keyCode) {
        getFilter().filter(text.subSequence(start, end), this);
    }
    public void setEnableAutocomplete(boolean isAutocompleteEnabled) {
        this.isAutocompleteEnabled = isAutocompleteEnabled;
    }

    public void setSentenceWords(List<String> words) {
        WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), words);
        adapter.setIsWordTagged(true);
//        recyclerView.setAdapter(adapter);
    }

    public void setAutoCompleteWords(Trie<String, String> wordTrie) {
        mWordTrie = wordTrie;

        WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), new ArrayList<String>(wordTrie.prefixMap("").keySet()));
        adapter.setIsWordTagged(false);
//        recyclerView.setAdapter(adapter);

        mFilter = getFilter();
    }

    public void setWordTagMap(String[] wordTagMap) {

        mFilter = getFilter();
    }


    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new TrieFilter();
        }
        return mFilter;
    }

    public void setTokenizer(MultiAutoCompleteTextView.Tokenizer t) {
        mTokenizer = t;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void replaceText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd() - 1;
        if (end == -1) end = 0;
        int start = mTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        String original = TextUtils.substring(editable, start, end);

        QwertyKeyListener.markAsReplaced(editable, start, end, original);
        editable.replace(start, end, mTokenizer.terminateToken(text));
    }

    private class WordTagFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

        }
    }

    private class TrieFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (prefix == null || prefix.length() == 0) {
                Trie<String, String> values = mWordTrie;;

                Set<String> set = values.prefixMap("").keySet();
                final ArrayList<String> newValues = new ArrayList<String>(set);

                results.values = newValues;
                results.count = newValues.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();

                Trie<String, String> values = mWordTrie;;

                final int count = values.size();

                Set<String> set = values.prefixMap(prefix.toString()).keySet();
                final ArrayList<String> newValues = new ArrayList<String>(set);

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), (List<String>) results.values);
//            recyclerView.setAdapter(adapter);
            onFilterComplete(results.count);
        }
    }

}
