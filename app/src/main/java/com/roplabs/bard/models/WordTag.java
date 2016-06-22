package com.roplabs.bard.models;


public class WordTag  {
    public String word;
    public String tag;

    public WordTag(String word) {
        this.word = word;
        this.tag = "";
    }

    @Override
    public String toString() {
        return word + ":" + tag;
    }
}
