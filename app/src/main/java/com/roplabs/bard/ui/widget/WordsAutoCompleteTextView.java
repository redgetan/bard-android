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
    private String lastString;
    private String separator = " ";
    private String separatorRegex = "\\s+";
    private boolean isFormattingLocked = false;

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
        setMovementMethod(LinkMovementMethod.getInstance());

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

//        if (getText().length() > 0 && !getText().equals(lastString)) {
//            format();
//        }
    }

    public boolean containsInvalidWord() {
        String fullString = getText().toString();

        if (fullString.isEmpty()) return false;

        String[] strings = fullString.split(separatorRegex);


        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            if (!isWordValid(string)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWordValid(String word) {
        return mWordTrie.containsKey(normalizeWord(word));
    }

    private String normalizeWord(String word) {
        return word.toLowerCase().replaceAll("[\"\'.?!]","");
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

    // http://stackoverflow.com/a/38241477
    public void format() {
        int origCursorPosition = getSelectionEnd();
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
        setSelection(origCursorPosition);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }



    public View createTokenView(String text) {


        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setBackgroundResource(R.drawable.bordered_rectangle_rounded_corners);
//        ViewGroup.LayoutParams params = l.getLayoutParams();
//        params.height = (int) (params.height / 1.75); // half of before
//        l.setLayoutParams(params);

        TextView tv = new TextView(getContext());
        l.addView(tv);
        tv.setText(text);
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

    public int findTokenStart(int cursor) {
        return getTokenizer().findTokenStart(getText(), cursor);
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
                Trie<String, String> values = mWordTrie;

                Set<String> set = values.prefixMap("").keySet();
                final ArrayList<String> newValues = new ArrayList<String>(set);

                results.values = newValues;
                results.count = newValues.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();

                Trie<String, String> values = mWordTrie;

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
            if (results.values != null) {
                BardLogger.log("publishing filter: " + ((List<String>) results.values).toString());
            }
            WordListAdapter adapter = new WordListAdapter(ClientApp.getContext(), (List<String>) results.values);
            recyclerView.setAdapter(adapter);
            onFilterComplete(results.count);
        }
    }

}
