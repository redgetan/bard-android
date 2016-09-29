package com.roplabs.bard.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Segment {

    public static final String cdnPath = "https://segments.bard.co";

    private String word;
    private String sourceUrl;
    private String filePath;
    private String error;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public static List<Segment> buildFromWordTagList(List<WordTag> wordTagList) {
        List<Segment> segments = new ArrayList<Segment>();

        for (WordTag wordTag : wordTagList) {
            Segment segment = buildFromWordTagString(wordTag.toString());
            segments.add(segment);
        }

        return segments;
    }

    public static Segment buildFromWordTagString(String wordTagString) {
        Segment segment =  new Segment();
        String[] tokens = wordTagString.split(":");
        String word = tokens[0];
        String tag = tokens[1];
        segment.setWord(word);
        segment.setSourceUrl(sourceUrlFromWordTagString(wordTagString));
        return segment;
    }

    public static String sourceUrlFromWordTagString(String wordTagString) {
        Scene givenScene = Scene.forWordTagString(wordTagString);

        String tag = wordTagString.split(":")[1];

        return cdnPath + "/segments/" +  givenScene.getToken() + "/" + tag + ".mp4";
    }



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
