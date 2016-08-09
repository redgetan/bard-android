package com.roplabs.bard.models;

import com.google.gson.annotations.SerializedName;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene extends RealmObject {

    @PrimaryKey
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

    public static RealmResults<Scene> forCharacterToken(String characterToken) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).equalTo("characterToken", characterToken).findAll();
    }

    public static void copyToRealmOrUpdate(List<Scene> scenes) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        for (Scene scene : scenes) {
            Scene obj = Scene.forToken(scene.getToken());
            if (obj == null) {
                Scene.create(realm, scene.getToken(), scene.getCharacterToken(), scene.getName(), scene.getThumbnailUrl());
            }
        }

        realm.commitTransaction();
    }

    public static void create(Realm realm, String token, String characterToken, String name, String thumbnailUrl) {
        Scene scene = realm.createObject(Scene.class);
        scene.setToken(token);
        scene.setName(name);
        scene.setCharacterToken(characterToken);
        scene.setThumbnailUrl(thumbnailUrl);
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
