package com.roplabs.bard.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class WordTagSelector {
    HashMap<String, ArrayList<WordTag>> wordTagMap;
    private int currentWordTagIndex;
    private String currentWord;
    private int currentScrollPosition;

    private static final String NEXT_DIRECTION = "next";
    private static final String PREV_DIRECTION = "prev";

    public WordTagSelector(List<String> wordTags) {
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

    public void setCurrentWordTagIndex(int index) {
        this.currentWordTagIndex = index;
    }

    public int getCurrentWordTagIndex() {
        return currentWordTagIndex;
    }

    public int getCurrentWordTagCount() {
        return wordTagMap.get(currentWord).size();
    }

    public void initWordTagMap(List<String> wordTags) {
        this.wordTagMap = new HashMap<String, ArrayList<WordTag>>();

        ArrayList<WordTag> wordTagList;

        int i = 0;
        while (i < wordTags.size()) {
            String wordTagString = wordTags.get(i);
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

    public boolean setWordTag(WordTag wordTag) {
        boolean wordTagValid = (wordTag != null) && (!wordTag.tag.isEmpty());

        if (wordTagValid) {
            currentWord = wordTag.word;
            currentWordTagIndex = wordTagMap.get(currentWord).indexOf(wordTag);
            return true;
        }

        return false;
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
    // if exists, update character
    // return word
    public WordTag findWord(String word, String direction) {
        if (isWordNotInDatabase(word)) return null;

        currentWord = word;

        if (isWordChanged(word)) {
            resetWordTagIndex();
        } else {
            updateWordTagIndex(word, direction);
        }

        WordTag wordTag = getWordTagFromMap(word, currentWordTagIndex);
        setCurrentScrollPosition(wordTag.position);

        return wordTag;
    }

    public WordTag findRandomWord(String word) {
        if (isWordNotInDatabase(word)) return null;

        currentWord = word;
        currentWordTagIndex = new Random().nextInt(getCurrentWordTagCount());

        WordTag wordTag = getWordTagFromMap(word, currentWordTagIndex);
        setCurrentScrollPosition(wordTag.position);

        return wordTag;
    }

    public WordTag getWordTagFromMap(String word, int index) {
        List<WordTag> list = wordTagMap.get(word);

        WordTag wordTag = list.get(index);
        return new WordTag(wordTag.toString()); // return a copy (we dont want original wordTag from dict to be modified)
    }

    private boolean isWordNotInDatabase(String word) {
       return !wordTagMap.containsKey(word);
    }

    private boolean isWordChanged(String word) {
        return !currentWord.equals(word);
    }

    private void resetWordTagIndex() {
        currentWordTagIndex = 0;
    }

    private void updateWordTagIndex(String word, String direction) {
        // reset character if word changed
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
