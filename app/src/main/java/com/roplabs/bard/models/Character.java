package com.roplabs.bard.models;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class Character extends RealmObject implements Serializable {

    private String token;
    private String name;
    private String description;
    private Boolean isBundleDownloaded;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Character(){

    }

    public Character(String name, String description){
        this.name = name;
        this.description = description;
    }

    public static RealmResults<Character> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).findAll();
    }

    public static Character findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).findFirst();
    }

    public static Character forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Character.class).equalTo("token", token).findFirst();
    }

    public static void create(String token, String name, String description) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Character character = realm.createObject(Character.class);
        character.setToken(token);
        character.setName(name);
        character.setDescription(description);

        realm.commitTransaction();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsBundleDownloaded() {
        return this.isBundleDownloaded;
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

}
