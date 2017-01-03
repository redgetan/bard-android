package com.roplabs.bard.models;

import com.google.gson.annotations.SerializedName;
import io.realm.*;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

import java.io.Serializable;
import java.util.*;

public class Scene extends RealmObject {

    @PrimaryKey
    @Required
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
        return realm.where(Scene.class).findAllSorted("createdAt", Sort.DESCENDING);
    }

    public static Scene findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).findFirst();
    }

    public static Scene forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).equalTo("token", token).findFirst();
    }

    public static Scene forWordTagString(String wordTagString) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).contains("wordList", wordTagString).findFirst();
    }

    public static RealmResults<Scene>  forCharacterToken(String characterToken) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Scene.class).equalTo("characterToken", characterToken).findAll();
    }

    public static void createOrUpdate(List<Scene> scenes) {
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

    public static void setNameAndThumbnails(List<Scene> scenes) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        for (Scene scene : scenes) {
            Scene obj = Scene.forToken(scene.getToken());
            // obj is null if there's a new scene that was not previously downloaded during BardEditorActivity#initCharacterWordList
            if (obj != null) {
                if (obj.getName().isEmpty()) {
                    obj.setName(scene.getName());
                }

                if (obj.getThumbnailUrl().isEmpty()) {
                    obj.setThumbnailUrl(scene.getThumbnailUrl());
                }
            }
        }

        realm.commitTransaction();
    }

    public static Scene create(Realm realm, String token, String characterToken, String name, String thumbnailUrl) {
        Scene scene = realm.createObject(Scene.class);
        scene.setToken(token);
        scene.setName(name);
        scene.setCharacterToken(characterToken);
        scene.setThumbnailUrl(thumbnailUrl);
        scene.setCreatedAt(new Date(System.currentTimeMillis()));
        return scene;
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
        if (this.wordList == null) return "";
        return this.wordList;
    }

    public List<String> getWordListAsList() {
        return Arrays.asList(this.wordList.split(","));
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
