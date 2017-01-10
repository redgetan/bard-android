package com.roplabs.bard.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.roplabs.bard.util.Helper.normalizeWord;

public class WordTagSelector {
    HashMap<String, List<WordTag>> wordTagMap;
    HashMap<String, List<WordTag>> sceneWordTagMap;
    private int currentWordTagIndex;
    private String currentWord;
    private int currentScrollPosition;

    private static final String NEXT_DIRECTION = "next";
    private static final String PREV_DIRECTION = "prev";

    public WordTagSelector(List<String> wordTags) {
        this.currentWord = "";
        this.currentWordTagIndex = 0;
        this.currentScrollPosition = -1;
        this.sceneWordTagMap = new HashMap<String, List<WordTag>>();

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
        List<WordTag> wordTagList = wordTagMap.get(currentWord);
        if (wordTagList == null) {
            return 0;
        } else {
            return wordTagList.size();
        }
    }

    public void initWordTagMap(List<String> wordTags) {
        this.wordTagMap = new HashMap<String, List<WordTag>>();

        List<WordTag> wordTagList;

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

    public void setSceneWordTagMap(List<String> wordTags) {
        this.sceneWordTagMap = new HashMap<String, List<WordTag>>();

        List<WordTag> wordTagList;

        int i = 0;
        while (i < wordTags.size()) {
            String wordTagString = wordTags.get(i);
            WordTag wordTag = new WordTag(wordTagString, i);

            if ((wordTagList = this.sceneWordTagMap.get(wordTag.word)) != null) {
                wordTagList.add(wordTag);
            } else {
                wordTagList = new ArrayList<WordTag>();
                wordTagList.add(wordTag);
                this.sceneWordTagMap.put(wordTag.word, wordTagList);
            }

            i++;
        }
    }

    public void setSceneWordTagMap(HashMap<String, List<WordTag>> sceneWordTagMap) {
        this.sceneWordTagMap = sceneWordTagMap;
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

    public WordTag getWordTagFromWordTagString(String wordTagString) {
        if (!wordTagString.contains(":")) return null;

        String word = wordTagString.split(":")[0];
        String tag = wordTagString.split(":")[1];

        List<WordTag> wordTagList = wordTagMap.get(word);
        for (WordTag wordTag : wordTagList) {
            if (wordTag.tag.equals(tag)) {
                return wordTag;
            }
        }

        return null;
    }

    public void clearWordTag() {
        currentWord = "";
        currentWordTagIndex = 0;
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
        word = normalizeWord(word);
        if (isWordNotInDatabase(word)) return null;

        currentWord = word;

        if (isWordChanged(word)) {
            resetWordTagIndex();
        } else {
            updateWordTagIndex(word, direction);
        }

        WordTag wordTag = getWordTagFromList(wordTagMap.get(word), currentWordTagIndex);
        setCurrentScrollPosition(wordTag.position);

        return wordTag;
    }

    public WordTag findRandomWord(String word) {
        word = normalizeWord(word);
        if (isWordNotInDatabase(word)) return null;

        int randomIndex;
        WordTag wordTag;
        List<WordTag> wordTagList = wordTagMap.get(word);

        if (!sceneWordTagMap.isEmpty()) {
            // get scene specific word variants of word
            List<WordTag> sceneWordTagList = sceneWordTagMap.get(word);
            if (sceneWordTagList != null) {
                randomIndex = new Random().nextInt(sceneWordTagList.size());
                wordTag = getWordTagFromList(sceneWordTagList, randomIndex);
            } else {
                // if sceneWordTagMap fails to find appropriate word, fall back to general wordTagMap
                randomIndex = new Random().nextInt(wordTagList.size());
                wordTag = getWordTagFromList(wordTagList, randomIndex);
            }
        } else {
            randomIndex = new Random().nextInt(wordTagList.size());
            wordTag = getWordTagFromList(wordTagList, randomIndex);
        }


        currentWord = word;
        currentWordTagIndex = wordTagList.indexOf(wordTag);

        setCurrentScrollPosition(wordTag.position);

        return wordTag;
    }

    public WordTag getWordTagFromList(List<WordTag> wordTagList, int index) {
        WordTag wordTag = wordTagList.get(index);
        return new WordTag(wordTag); // return a copy (we dont want original wordTag from dict to be modified)
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
