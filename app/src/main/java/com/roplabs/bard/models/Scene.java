package com.roplabs.bard.models;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class Scene extends RealmObject implements Serializable {

    private String token;
    private String name;
    private String characterToken;
    private String wordList;
    private String thumbnailUrl;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Scene(){

    }

    public Scene(String name){
        this.name = name;
    }

    public static RealmResults<Scene> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).findAll();
    }

    public static Scene findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).findFirst();
    }

    public static Scene forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).equalTo("token", token).findFirst();
    }

    public static void copyToRealmOrUpdate(List<Scene> scenes) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(scenes);
        realm.commitTransaction();
    }

    public static void create(String token, String characterToken, String name, String thumbnailUrl) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Scene scene = realm.createObject(Scene.class);
        scene.setToken(token);
        scene.setName(name);
        scene.setCharacterToken(characterToken);
        scene.setThumbnailUrl(characterToken);

        realm.commitTransaction();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCharacterToken(String characterToken) {
        this.characterToken = characterToken;
    }

    public String getCharacterToken() {
        return this.characterToken;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailUrl() {
        return this.thumbnailUrl;
    }

    public void setWordList(String wordList) {
        this.wordList = wordList;
    }

    public String getWordList() {
        return this.wordList;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
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
