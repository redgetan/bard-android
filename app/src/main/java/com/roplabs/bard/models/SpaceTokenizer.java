package com.roplabs.bard.models;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

import java.util.HashMap;

// http://stackoverflow.com/a/4596652
public class SpaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {

    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && text.charAt(i - 1) != ' ') {
            i--;
        }
        while (i < cursor && text.charAt(i) == ' ') {
            i++;
        }

        return i;
    }

    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (text.charAt(i) == ' ') {
                return i;
            } else {
                i++;
            }
        }

        return len;
    }

    // cases:
    //   1. word at beginning of string
    //   2. word at middle of string

    public static HashMap<String, Integer> findStartStopOfNthToken(CharSequence text, int tokenIndex) {
        int i = 0;
        int start = -1;
        int stop = -1;
        int nthToken = 0;
        boolean shouldFindStart = true;
        boolean shouldFindStop = false;

        while (i < text.length()) {
            // non-space character
            if (text.charAt(i) != ' ') {
                if (shouldFindStart) {
                    start = i;
                    shouldFindStart = false;
                    shouldFindStop = true;
                }
            } else {
                // space character
                if (shouldFindStop) {
                    stop = i;
                    shouldFindStop = false;
                    shouldFindStart = true;
                }
            }

            if (start >= 0 && stop >= 0) {
                if (tokenIndex == nthToken) {
                    break;
                } else {
                    // reset for next round
                    start = -1;
                    stop  = -1;
                    nthToken++;
                }
            }

            i++;
        }

        // handles case where tokenindex corresponds to lastword (and no space is found on last word)
        if (stop == -1) stop = text.length();

        HashMap<String, Integer> result = new HashMap<String, Integer>();
        result.put("start",start);
        result.put("stop",stop);

        return result;
    }

    public CharSequence terminateToken(CharSequence text) {
        int i = text.length();

        while (i > 0 && text.charAt(i - 1) == ' ') {
            i--;
        }

        if (i > 0 && text.charAt(i - 1) == ' ') {
            return text;
        } else {
            if (text instanceof Spanned) {
                SpannableString sp = new SpannableString(text + " ");
                TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                        Object.class, sp, 0);
                return sp;
            } else {
                return text + " ";
            }
        }
    }
}
