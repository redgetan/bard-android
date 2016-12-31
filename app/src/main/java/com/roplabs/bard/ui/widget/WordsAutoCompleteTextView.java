package com.roplabs.bard.ui.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.adapters.WordListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.events.PreviewWordEvent;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.BardLogger;
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
    private MultiAutoCompleteTextView.Tokenizer mTokenizer;
    RecyclerView recyclerView;
    private Filter mFilter;
    private Trie<String,String> mWordTrie;
    private boolean isAutocompleteEnabled;
    private boolean isFindInPage;
    private int lastStart;
    private int lastEnd;

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
    }

    @Override
    public void onFilterComplete(int count) {

    }

    public String getNextChar(CharSequence s, int start) {
        String result = "";
        String character = getAddedChar(start);
        boolean isBackspacePressed = character.equals("");

        if (start == length() - 1 || start == length()) {
            result = "";
        } else if (isBackspacePressed)  {
            result = getText().toString().subSequence(start, start + 1).toString();
        } else {
            result = getText().toString().subSequence(start + 1, start + 2).toString();
        }
        return result.toLowerCase();
    }

    public String getPrevWord(int start) {
        String[] tokens = getText().toString().subSequence(0,start).toString().trim().split("\\s+");
        return tokens[tokens.length - 1].toLowerCase();
    }

    private class MyWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            doAfterTextChanged();
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {

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

    public  MultiAutoCompleteTextView.Tokenizer getTokenizer() {
        return mTokenizer;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    /**
     * MODIFIED: on last line "editable.replace", we replace with text instead of mTokenizer.terminateToken(text)
     *
     * <p>Performs the text completion by replacing the range from
     * {@link MultiAutoCompleteTextView.Tokenizer#findTokenStart} to {@link #getSelectionEnd} by the
     * the result of passing <code>text</code> through
     * {@link MultiAutoCompleteTextView.Tokenizer#terminateToken}.
     * In addition, the replaced region will be marked as an AutoText
     * substition so that if the user immediately presses DEL, the
     * completion will be undone.
     * Subclasses may override this method to do some different
     * insertion of the content into the edit box.</p>
     *
     * @param text the selected suggestion in the drop down list
     */
    public void replaceText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        String original = TextUtils.substring(editable, start, end);

        QwertyKeyListener.markAsReplaced(editable, start, end, original);
        editable.replace(start, end, text);
    }

    public void replaceSelectedText(CharSequence text) {
        Editable editable = getText();
        editable.replace(getSelectionStart(),getSelectionEnd(), text);
    }

    public String getAddedChar(int start) {
        try {
            return getText().subSequence(start,getSelectionEnd()).toString().toLowerCase();
        } catch (StringIndexOutOfBoundsException e) {
            BardLogger.trace("getAddedChar: " + e.getMessage());
            return " ";
        }
    }

    public int getTokenIndex() {
        int end = getSelectionEnd();
        boolean isInFrontOfWord = mTokenizer.findTokenEnd(getText(), end + 1) > end;

        return getText().subSequence(0, end).toString().trim().split("\\s+").length - 1;
    }

    public int getTokenCount() {
        String[] tokens = getText().toString().trim().split("\\s+");
        if (tokens.length == 1) {
            return tokens[0].isEmpty() ? 0 : 1;
        } else {
            return tokens.length;
        }
    }

    public String getLastWord() {
        int start = mTokenizer.findTokenStart(getText(), getSelectionEnd());
        int end = mTokenizer.findTokenEnd(getText(), getSelectionEnd());

        if (end > start) {
            // 1st method
            return getText().subSequence(start, end).toString().toLowerCase();
        } else {
            // 2nd method (if cursor in empty space, such not near a word)
            String[] words = getText().subSequence(0, end).toString().trim().split("\\s+");
            return words[words.length - 1].toLowerCase();
        }

    }

    public void replaceLastText(CharSequence text) {
        clearComposingText();

        Editable editable = getText();
        editable.replace(lastStart, lastEnd, text);

        int end = lastEnd;
        lastStart = mTokenizer.findTokenStart(getText(), end);
        lastEnd   = mTokenizer.findTokenEnd(getText(), end);
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
