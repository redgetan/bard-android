package com.sandbox.myfirstapp.app.models;

import android.content.Context;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Index extends RealmObject {

    private String token;
    private String name;
    private String description;
    private String error;
    private String wordList;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Index(){

    }

    public Index(String name, String description){
        this.name = name;
        this.description = description;
    }

    public static RealmResults<Index> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Index.class).findAll();
    }

    public static Index findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Index.class).findFirst();
    }

    public static Index forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Index.class).equalTo("token", token).findFirst();
    }

    public static void create(String token, String name, String description, String wordList) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Index index = realm.createObject(Index.class);
        index.setToken(token);
        index.setName(name);
        index.setDescription(description);
        index.setWordList(wordList);

        realm.commitTransaction();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
     * The description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The word_list
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public String getWordList() {
        return this.wordList;
    }

    public void setWordList(String wordList) {
        this.wordList = wordList;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
