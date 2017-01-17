package com.roplabs.bard.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.method.QwertyKeyListener;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
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
import java.util.*;

import static com.roplabs.bard.util.Helper.normalizeWord;

public class WordsAutoCompleteTextView extends EditText implements Filterable, Filter.FilterListener {
    private MultiAutoCompleteTextView.Tokenizer mTokenizer;
    RecyclerView recyclerView;
    private Filter mFilter;
    private Trie<String,String> mWordTrie;
    private boolean isAutocompleteEnabled;
    private boolean isFindInPage;
    private int lastStart;
    private int lastEnd;
    private String lastString;
    private String separator = " ";
    private String separatorRegex = "\\s+";
    private boolean isFormattingLocked = false;
    private List<String> filteredResults;
    private List<String> originalWordTagStringList;

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

    private OnFilterCompleteListener filterCompleteListener;

    public interface OnFilterCompleteListener {
        void onFilterComplete(List<String> results);
    }

    public void setOnFilterCompleteListener(OnFilterCompleteListener listener) {
        this.filterCompleteListener = listener;
    }

    public void setOriginalWordTagStringList(List<String> originalWordTagStringList) {
        this.originalWordTagStringList = originalWordTagStringList;
    }

    public List<String> getOriginalWordTagStringList() {
        return originalWordTagStringList;
    }


    public void initWordsAutoComplete() {
        setMovementMethod(LinkMovementMethod.getInstance());

        this.filteredResults = new ArrayList<String>();
        this.isAutocompleteEnabled = true;
    }

    @Override
    public void onFilterComplete(int count) {
        List<String> results = getFilteredResults();
        this.filterCompleteListener.onFilterComplete(results);
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

    public void findPrefixMatches() {
        String text = getText().toString();
        int keyCode = 0;
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(text, end);
        if (start != -1 && end != -1 && !getText().subSequence(start,end).toString().contains(":")) {
            // make sure we're only doing autocomplete on words that haven't been tagged yet
            performFiltering(text, start, end, keyCode);
        }
    }

    public boolean containsInvalidWord() {
        String fullString = getText().toString();

        if (fullString.isEmpty()) return false;

        String[] strings = fullString.split(separatorRegex);

        for (int i = 0; i < strings.length; i++) {
            // only extract word part of wordTagString custom:alk234jisS
            String string = strings[i].split(":")[0];
            if (!isWordValid(string)) {
                return true;
            }
        }

        return false;
    }

    // needs to be in dictionary + must be tagged
    private boolean isWordValid(String word) {
        return mWordTrie.prefixMap(normalizeWord(word)).keySet().size() > 0 ;
    }

    public boolean isBeforeImageSpan() {
          // get all existing imagespans
        ImageSpan[] existingImageSpans = getText().getSpans(0, getText().length(), ImageSpan.class);

        for (ImageSpan existingImageSpan : existingImageSpans) {
            int imageSpanStart = getText().getSpanStart(existingImageSpan);
            if (getSelectionEnd() <= imageSpanStart ) {
                return true;
            }

        }

        return false;

    }

    public boolean isBeforeImageSpan(int cursorPosition) {
        // get all existing imagespans
        ImageSpan[] existingImageSpans = getText().getSpans(0, getText().length(), ImageSpan.class);

        for (ImageSpan existingImageSpan : existingImageSpans) {
            int imageSpanStart = getText().getSpanStart(existingImageSpan);
            if (cursorPosition <= imageSpanStart ) {
                return true;
            }

        }

        return false;

    }

    public boolean isImmediatelyAfterImageSpan() {
        // get all existing imagespans
        ImageSpan[] existingImageSpans = getText().getSpans(0, getText().length(), ImageSpan.class);

        for (ImageSpan existingImageSpan : existingImageSpans) {
            int imageSpanEnd = getText().getSpanEnd(existingImageSpan);
            if (getSelectionEnd() == imageSpanEnd ) {
                return true;
            }

        }

        return false;

    }

    // http://stackoverflow.com/a/38241477
    public void format() {
        // remember original cursor position to set it back later
        int origCursorPosition = getSelectionEnd();
        if (origCursorPosition == -1) return;

        SpannableStringBuilder sb = new SpannableStringBuilder();
        String fullString = getText().toString();

        if (fullString.isEmpty()) return;

        String[] strings = fullString.split(separatorRegex);


        for (int i = 0; i < strings.length; i++) {

            String string = strings[i];
            sb.append(string);

            int startIdx = sb.length() - (string.length());
            int endIdx = sb.length();

            // check if word is in dictionary, if not, color it red
            if (!isWordValid(string)) {
                sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ClientApp.getContext(), R.color.md_red_400)), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            // if it doesnt contain separator, dont add 'tag' span
            if (fullString.charAt(fullString.length() - 1) != separator.charAt(0) && i == strings.length - 1) {
                break;
            }

            BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(createTokenView(string));
            bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());

            sb.setSpan(new ImageSpan(bd), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            MyClickableSpan myClickableSpan = new MyClickableSpan(startIdx, endIdx);
            sb.setSpan(myClickableSpan, Math.max(endIdx-2, startIdx), endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (i < strings.length - 1) {
                sb.append(separator);
            } else if (fullString.charAt(fullString.length() - 1) == separator.charAt(0)) {
                sb.append(separator);
            }
        }


        lastString = sb.toString();

        setText(sb);

        int targetCursorPosition = origCursorPosition;

        if (targetCursorPosition > sb.length()) targetCursorPosition = sb.length();
        // if cursor is in immediately front of another word, move cursor back so that its before space character instead
        if (isBeforeImageSpan(targetCursorPosition) ) {
            if (getText().charAt(targetCursorPosition) != ' ') {
                targetCursorPosition = targetCursorPosition - 1;
            }
        }
        if (sb.length() == 0) targetCursorPosition = 0;

        setSelection(targetCursorPosition);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }



    public View createTokenView(String wordTagString) {

        String word = wordTagString.split(":")[0];

        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setBackgroundResource(R.drawable.bordered_rectangle_rounded_corners);
//        ViewGroup.LayoutParams params = l.getLayoutParams();
//        params.height = (int) (params.height / 1.75); // half of before
//        l.setLayoutParams(params);

        TextView tv = new TextView(getContext());
        l.addView(tv);
        tv.setText(word);
        tv.setClickable(true);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);

//        ImageView im = new ImageView(getContext());
//        l.addView(im);
//        im.setImageResource(R.drawable.ic_clear_black_18dp);
//        im.setScaleType(ImageView.ScaleType.FIT_CENTER);

        return l;
    }

    public Object convertViewToDrawable(View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(this.getHeight(), MeasureSpec.AT_MOST);
//        v.measure();

        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(b);

        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(getContext().getResources(), viewBmp);
    }

    private class MyClickableSpan extends ClickableSpan {

        int startIdx;
        int endIdx;

        public MyClickableSpan(int startIdx, int endIdx) {
            super();
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        @Override
        public void onClick(View widget) {



            String s = getText().toString();

            String s1 = s.substring(0, startIdx);
            String s2 = s.substring(Math.min(endIdx+1, s.length()-1), s.length() );

            WordsAutoCompleteTextView.this.setText(s1 + s2);
        }

    }

    protected void performFiltering(CharSequence text, int start, int end,
                                    int keyCode) {
        if (end == -1) return;
        getFilter().filter(text.subSequence(start, end), this);
    }
    public void setEnableAutocomplete(boolean isAutocompleteEnabled) {
        this.isAutocompleteEnabled = isAutocompleteEnabled;
    }

    public boolean isFilteredAlphabetically() {
        return this.isAutocompleteEnabled;
    }

    public void setSentenceWords(List<String> words) {
        WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), words);
        adapter.setIsWordTagged(true);
//        recyclerView.setAdapter(adapter);
    }

    public void setAutoCompleteWords(Trie<String, String> wordTrie) {
        mWordTrie = wordTrie;

//        WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), new ArrayList<String>(wordTrie.prefixMap("").keySet()));
//        adapter.setIsWordTagged(false);
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

    public String getLastChar() {
        if (getText().length() == 0) return "";

        return String.valueOf(getText().charAt(getText().length() - 1));
    }

    public int findTokenStart(int cursor) {
        return getTokenizer().findTokenStart(getText(), cursor);
    }

    public int findTokenEnd(int cursor) {
        return getTokenizer().findTokenEnd(getText(), cursor);
    }

    public int getTokenIndex() {
        int end = getSelectionEnd();
        boolean isInFrontOfWord = mTokenizer.findTokenEnd(getText(), end + 1) > end;

        if (end == -1) {
            return 0;
        }

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

    public boolean isUnfiltered() {
        List<String> results = ((WordListAdapter) recyclerView.getAdapter()).getList();
        return (results.size() == originalWordTagStringList.size()) || (results.size() == mWordTrie.size());
    }

    // if were not in autocomplete mode, we wont be setting recyclerView adapter list
    // thus, we dont be able to use getAdapter.getList to get filter results
    // instead, we would get it from the filteredResults variable, which is always stored during prefix filtering


    // if we are in autocomplete mode, there are 2 cases
    //   case 1: nothing is filtered (empty string), we want to return sequential list
    public List<String> getFilteredResults() {
        if (isAutocompleteEnabled) {
            List<String> results = ((WordListAdapter) recyclerView.getAdapter()).getList();
            return results;
        } else {
            return filteredResults;
        }
    }

    public String getClickedWordTag() {
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(),end);
        if (start == end) {
            // end is at beginning of word, so try to find actual end of word
            start = end;
            end = mTokenizer.findTokenEnd(getText(),start);
        }

        return getText().toString().substring(start, end);
    }

    public String getCurrentTokenWord() {
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(), end);

        if (end == -1) {
            // 2nd method
            String[] words = getText().toString().trim().split("\\s+");
            return words.length > 0 ? words[words.length - 1] : "";
        } else {
            Editable editable = getText();
            return TextUtils.substring(editable, start, end);
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

    public void displayOriginalWordList() {
        WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), originalWordTagStringList);
        adapter.setIsWordTagged(true);
        recyclerView.setAdapter(adapter);
    }

    // takes into account space
    // input:
    //   current_text: ("i am sam ")
    //   text: "yo"
    // result: "i am yo"
    public void replaceLastText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd();

        int start = mTokenizer.findTokenStart(getText(), end);
        if (start == end) {
            int newEnd = mTokenizer.findTokenEnd(getText(), start);
            if (newEnd != end) {
                // there's a word right after cursor (get this word)
                start = end;
                end = newEnd;
            } else {
                // no word after cursor, get nearest word before cursor
                if (end > 0 && getText().charAt(end - 1) == ' ') {
                    // if there's leading space, ignore it by subtracting 1
                    // charToAppend = " ";
                    end = end - 1;
                    start = mTokenizer.findTokenStart(getText(), end);
                    // hopefully start should != end by this time
                }

            }
        }

        Editable editable = getText();
        String original = TextUtils.substring(editable, start, end);

        QwertyKeyListener.markAsReplaced(editable, start, end, original);
        editable.replace(start, end, text);
        BardLogger.log("replaceLastText: [after]" + getText().toString());
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

            prefix = normalizeWord(prefix);

            if (prefix == null || prefix.length() == 0) {
                Trie<String, String> values = mWordTrie;

                Set<String> set = values.prefixMap("").keySet();
                final ArrayList<String> newValues = new ArrayList<String>(set);

                results.values = newValues;
                results.count = newValues.size();
            } else {
                String prefixString = prefix.toString();

                Trie<String, String> values = mWordTrie;

                final int count = values.size();

                Set<String> set = values.prefixMap(prefixString).keySet();
                final ArrayList<String> newValues = new ArrayList<String>(set);

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<String> actualResults = ((ArrayList<String>) results.values);
            if (results.values != null) {
//                BardLogger.log("publishing filter: " + actualResults.toString());
            }

            if (isAutocompleteEnabled) {
                WordListAdapter adapter;

                if (actualResults.size() == mWordTrie.size()) {
                    adapter = new WordListAdapter(ClientApp.getContext(), originalWordTagStringList);
                    adapter.setIsWordTagged(true);
                } else {
                    adapter = new WordListAdapter(ClientApp.getContext(), actualResults);
                }

                recyclerView.setAdapter(adapter);
            } else {
                filteredResults = actualResults;
            }
        }
    }

}
