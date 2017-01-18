package com.roplabs.bard.models;


public class WordTag  {
    public String word;
    public String tag;
    public int position; // optional
    public float duration;

    public WordTag(String word) {
        String[] tokens = word.split(":");
        if (tokens.length > 1) {
            this.word = tokens[0];
            this.tag = tokens[1];

            if (tokens.length == 3) {
                this.duration = Float.valueOf(tokens[2]);
            } else {
                this.duration = 1;
            }

        } else {
            this.word = word;
            this.tag = "";
        }
        this.position = -1;
    }

    public WordTag(String word, int position) {
        this(word);
        this.position = position;
    }

    // copy constructor
    public WordTag(WordTag another) {
        this.word      = another.word;
        this.tag       = another.tag;
        this.position  = another.position;
        this.duration  = another.duration;
    }

    public boolean isFilled() {
        return this.tag.length() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WordTag wordTag = (WordTag) o;

        if (!word.equals(wordTag.word)) return false;
        return tag.equals(wordTag.tag);

    }

    @Override
    public int hashCode() {
        int result = word.hashCode();
        result = 31 * result + tag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return word + ":" + tag;
    }
}
