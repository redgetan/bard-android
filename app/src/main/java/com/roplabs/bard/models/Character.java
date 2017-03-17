package com.roplabs.bard.models;

import io.realm.*;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class Character extends RealmObject {

    @PrimaryKey
    @Required
    private String token;
    private String name;
    private String details;
    private String thumbnailUrl;
    private String wordListByScene;
    private Integer timestamp;
    private Boolean isBundleDownloaded;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Character(){

    }

    public Character(String name, String details){
        this.name = name;
        this.details = details;
    }

    public static RealmResults<Character> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).findAllSorted("createdAt", Sort.DESCENDING);
    }

    public static Character findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).findFirst();
    }

    public static Character forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).equalTo("token", token).findFirst();
    }

    public static void createOrUpdate(List<Character> characters) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        for (Character character : characters) {
            Character obj = Character.forToken(character.getToken());
            if (obj == null) {
                Character.create(realm, character.getToken(), character.getName(), character.getDetails(), character.getThumbnailUrl(), character.getTimestamp());
            } else {
                obj.setName(character.getName());
                obj.setThumbnailUrl(character.getThumbnailUrl());
            }
        }
        realm.commitTransaction();
    }

    public static void create(Realm realm, String token, String name, String details, String thumbnailUrl, Integer timestamp) {
        Character character = realm.createObject(Character.class);
        character.setIsBundleDownloaded(false);
        character.setToken(token);
        character.setName(name);
        character.setThumbnailUrl(thumbnailUrl);
        character.setTimestamp(timestamp);
        character.setDetails(details);
        character.setCreatedAt(new Date(System.currentTimeMillis()));
    }

    public Integer getTimestamp() {
        if (this.timestamp == null) return -1;
        return this.timestamp;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Map<String, String> getWordListByScene() {
        try {
            if (wordListByScene == null) {
                return new HashMap<String, String>();
            } else {
                return jsonToMap(wordListByScene);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return new HashMap<String, String>();
        }
    }

    public void setWordListByScene(Map<String, String> wordListByScene) {
        this.wordListByScene = new JSONObject(wordListByScene).toString();
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsBundleDownloaded() {
        return this.isBundleDownloaded && wordListByScene != null;
    }

    public void setIsBundleDownloaded(Boolean isBundleDownloaded) {
        this.isBundleDownloaded = isBundleDownloaded;
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

    public static Map<String, String> jsonToMap(String t) throws JSONException {

        Map<String, String> map = new HashMap<String, String>();
        JSONObject jObject = new JSONObject(t);
        Iterator<String> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = keys.next();
            String value = jObject.getString(key);
            map.put(key, value);
        }

        return map;
    }

}
