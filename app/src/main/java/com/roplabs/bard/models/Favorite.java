package com.roplabs.bard.models;

import com.google.gson.annotations.SerializedName;
import com.roplabs.bard.ClientApp;
import io.realm.*;
import io.realm.annotations.Ignore;

import java.io.Serializable;
import java.util.*;

public class Favorite extends RealmObject {

    private String sceneToken;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Favorite(){

    }

    public static RealmResults<Favorite> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Favorite.class).findAllSorted("createdAt", Sort.DESCENDING);
    }

    public static Favorite findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Favorite.class).findFirst();
    }

    public static RealmResults<Favorite> forUsername(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Favorite> results = realm.where(Favorite.class)
                .equalTo("username", username)
                .findAllSorted("createdAt", Sort.DESCENDING);

        return results;
    }

    public static Favorite forSceneTokenAndUsername(String sceneToken, String username) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Favorite.class)
                    .equalTo("sceneToken", sceneToken)
                    .equalTo("username", username)
                    .findFirst();
    }

    public static void createOrUpdate(List<Scene> scenes) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        String username = Setting.getUsername(ClientApp.getContext());

        for (Scene scene : scenes) {
            Favorite obj = Favorite.forSceneTokenAndUsername(scene.getToken(), username);
            if (obj == null) {
                Favorite.create(realm, scene.getToken(), username);
            }
        }

        realm.commitTransaction();
    }

    public static Favorite create(Realm realm, String sceneToken, String username) {
        Favorite favorite = realm.createObject(Favorite.class);
        favorite.setSceneToken(sceneToken);
        favorite.setUsername(username);
        return favorite;
    }

    public void setSceneToken(String sceneToken) {
        this.sceneToken = sceneToken;
    }

    public String getSceneToken() {
        return this.sceneToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
