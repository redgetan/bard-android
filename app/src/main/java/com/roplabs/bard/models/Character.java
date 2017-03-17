package com.roplabs.bard.models;

import io.realm.*;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class Character extends RealmObject {

    @PrimaryKey
    @Required
    private String token;
    private String name;
    private String details;
    private String thumbnailUrl;
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
                Character.create(realm, character.getToken(), character.getName(), character.getDetails(), character.getThumbnailUrl());
            } else {
                obj.setName(character.getName());
                obj.setThumbnailUrl(character.getThumbnailUrl());
            }
        }
        realm.commitTransaction();
    }

    public static void create(Realm realm, String token, String name, String details, String thumbnailUrl) {
        Character character = realm.createObject(Character.class);
        character.setIsBundleDownloaded(false);
        character.setToken(token);
        character.setName(name);
        character.setThumbnailUrl(thumbnailUrl);
        character.setDetails(details);
        character.setCreatedAt(new Date(System.currentTimeMillis()));
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
