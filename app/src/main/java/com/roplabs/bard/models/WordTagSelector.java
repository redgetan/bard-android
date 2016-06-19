package com.roplabs.bard.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WordTagSelector {
    HashMap<String, ArrayList<String>> wordTagMap;
    private int currentWordTagIndex;
    private String currentWord;

    private static final String NEXT_DIRECTION = "next";
    private static final String PREV_DIRECTION = "prev";

    public WordTagSelector(String[] wordTags) {
        this.currentWord = "";
        this.currentWordTagIndex = 0;

        initWordTagMap(wordTags);
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public int getCurrentWordTagIndex() {
        return currentWordTagIndex;
    }

    public int getCurrentWordTagCount() {
        return wordTagMap.get(currentWord).size();
    }

    public void initWordTagMap(String[] wordTags) {
        this.wordTagMap = new HashMap<String, ArrayList<String>>();

        ArrayList<String> wordTagList;

        int i = 0;
        while (i < wordTags.length) {
            String wordTag = wordTags[i];
            String word = wordTag.split(":")[0];

            if ((wordTagList = wordTagMap.get(word)) != null) {
                wordTagList.add(wordTag);
            } else {
                wordTagList = new ArrayList<String>();
                wordTagList.add(wordTag);
                wordTagMap.put(word, wordTagList);
            }

            i++;
        }
    }

    public String getCurrentWordTag() {
       return wordTagMap.get(currentWord).get(currentWordTagIndex);
    }

    public void setWordTag(String wordTagString) {
        currentWord = wordTagString.split(":")[0];
        currentWordTagIndex = wordTagMap.get(currentWord).indexOf(wordTagString);
    }

    public String findNextWord(String word) {
        return findWord(word, NEXT_DIRECTION);
    }

    public String findNextWord() {
        return findWord(currentWord, NEXT_DIRECTION);
    }

    public String findPrevWord(String word) {
        return findWord(word, PREV_DIRECTION);
    }

    public String findPrevWord() {
        return findWord(currentWord, PREV_DIRECTION);
    }

    // see if word exists on the map
    // if exists, update index
    // return word
    public String findWord(String word, String direction) {
        if (isWordNotInDatabase(word)) return "";

        if (isWordChanged(word)) {
            currentWord = word;
            currentWordTagIndex = 0;
        } else {
            updateWordTagIndex(word, direction);
        }

        return getCurrentWordTag();
    }

    private boolean isWordNotInDatabase(String word) {
       return !wordTagMap.containsKey(word);
    }

    private boolean isWordChanged(String word) {
        return !currentWord.equals(word);
    }

    private void updateWordTagIndex(String word, String direction) {
        // reset index if word changed
        if (direction.equals(NEXT_DIRECTION)) {
            currentWordTagIndex++;
        } else if (direction.equals(PREV_DIRECTION)) {
            currentWordTagIndex--;
        }

        // handle out of bounds conditions
        if (currentWordTagIndex < 0) {
            currentWordTagIndex = wordTagMap.get(word).size() - 1;
        } else if (currentWordTagIndex > wordTagMap.get(word).size() - 1){
            currentWordTagIndex = 0;
        }
    }

}
