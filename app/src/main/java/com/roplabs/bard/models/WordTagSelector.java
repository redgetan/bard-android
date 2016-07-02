package com.roplabs.bard.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WordTagSelector {
    HashMap<String, ArrayList<WordTag>> wordTagMap;
    private int currentWordTagIndex;
    private String currentWord;
    private int currentScrollPosition;

    private static final String NEXT_DIRECTION = "next";
    private static final String PREV_DIRECTION = "prev";

    public WordTagSelector(String[] wordTags) {
        this.currentWord = "";
        this.currentWordTagIndex = 0;
        this.currentScrollPosition = -1;

        initWordTagMap(wordTags);
    }

    public void setCurrentScrollPosition(int position) {
        this.currentScrollPosition = position;
    }

    public int getCurrentScrollPosition() {
        return this.currentScrollPosition;
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
        this.wordTagMap = new HashMap<String, ArrayList<WordTag>>();

        ArrayList<WordTag> wordTagList;

        int i = 0;
        while (i < wordTags.length) {
            String wordTagString = wordTags[i];
            WordTag wordTag = new WordTag(wordTagString, i);

            if ((wordTagList = wordTagMap.get(wordTag.word)) != null) {
                wordTagList.add(wordTag);
            } else {
                wordTagList = new ArrayList<WordTag>();
                wordTagList.add(wordTag);
                wordTagMap.put(wordTag.word, wordTagList);
            }

            i++;
        }
    }

    public WordTag getCurrentWordTag() {
       return wordTagMap.get(currentWord).get(currentWordTagIndex);
    }

    public void setWordTag(WordTag wordTag) {
        currentWord = wordTag.word;
        currentWordTagIndex = wordTagMap.get(currentWord).indexOf(wordTag);
    }

    public WordTag findNextWord(String word) {
        return findWord(word, NEXT_DIRECTION);
    }

    public WordTag findNextWord() {
        return findWord(currentWord, NEXT_DIRECTION);
    }

    public WordTag findPrevWord(String word) {
        return findWord(word, PREV_DIRECTION);
    }

    public WordTag findPrevWord() {
        return findWord(currentWord, PREV_DIRECTION);
    }

    // see if word exists on the map
    // if exists, update index
    // return word
    public WordTag findWord(String word, String direction) {
        if (isWordNotInDatabase(word)) return null;

        if (isWordChanged(word)) {
            currentWord = word;
            currentWordTagIndex = 0;
        } else {
            updateWordTagIndex(word, direction);
        }

        WordTag wordTag = getCurrentWordTag();
        setCurrentScrollPosition(wordTag.position);

        return wordTag;
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
