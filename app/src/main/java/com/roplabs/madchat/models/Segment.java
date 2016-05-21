package com.roplabs.madchat.models;

import java.util.HashMap;
import java.util.Map;

public class Segment {

    private String word;
    private String sourceUrl;
    private String filePath;
    private String error;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The word
     */
    public String getWord() {
        return word;
    }

    /**
     *
     * @param word
     * The word
     */
    public void setWord(String word) {
        this.word = word;
    }

    /**
     *
     * @return
     * The sourceUrl
     */
    public String getSourceUrl() {
        return sourceUrl;
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
    public void setError(String error) {}


        /**
         *
         * @param sourceUrl
         * The sourceUrl
         */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
