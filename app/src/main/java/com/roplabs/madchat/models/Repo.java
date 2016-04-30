package com.roplabs.madchat.models;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Repo extends RealmObject {

    private String token;
    private String url;
    private String filePath;
    private String wordList;
    private String error;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Repo(){

    }

    public Repo(String url, String filePath, String wordList, Date createdAt){
        this.url = url;
        this.filePath = filePath;
        this.wordList = wordList;
        this.createdAt = createdAt;
    }

    public static RealmResults<Repo> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Repo.class).findAll();
    }

    public static void create(String token, String videoUrl, String videoPath, String wordList, Date createdAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Repo index = realm.createObject(Repo.class);

        index.setToken(token);
        index.setUrl(videoUrl);
        index.setFilePath(videoPath);
        index.setWordList(wordList);
        index.setCreatedAt(createdAt);

        realm.commitTransaction();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
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

    }

    /**
     *
     * @return createdAt
     * The url
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     *
     * @param createdAt
     * The url
     */
    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
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

    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }


}