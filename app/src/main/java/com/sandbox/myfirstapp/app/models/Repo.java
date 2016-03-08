package com.sandbox.myfirstapp.app.models;

import java.util.HashMap;
import java.util.Map;

public class Repo {

    private String url;
    private String wordList;
    private String error;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url
     * The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     *
     * @return
     * The error
     */
    public String getError() {
        return error;
    }

    /**
     *
     * @param error
     * The error
     */
    public void setError(String error) {
        this.error = error;
    }
    /**
     *
     * @return
     * The wordList
     */
    public String getWordList() {
        return wordList;
    }

    /**
     *
     * @param wordList
     * The word_list
     */
    public void setWordList(String wordList) {
        this.wordList = wordList;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
