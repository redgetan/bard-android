package com.roplabs.bard.models;

import com.roplabs.bard.ClientApp;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;

import java.util.*;

public class Like extends RealmObject {

    private String repoToken;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Like(){

    }

    public static RealmResults<Like> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Like.class).findAllSorted("createdAt", Sort.DESCENDING);
    }

    public static Like findFirst() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Like.class).findFirst();
    }

    public static RealmResults<Like> forUsername(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Like> results = realm.where(Like.class)
                .equalTo("username", username)
                .findAllSorted("createdAt", Sort.DESCENDING);

        return results;
    }

    public static List<String> repoTokensForUsername(String username) {
        RealmResults<Like> likes = forUsername(username);
        List<String> repoTokens = new ArrayList<String>();

        for (Like like : likes) {
            repoTokens.add(like.getRepoToken());
        }

        return repoTokens;
    }

    public static Like forRepoTokenAndUsername(String repoToken, String username) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Like.class)
                .equalTo("repoToken", repoToken)
                .equalTo("username", username)
                .findFirst();
    }

    public static void createOrUpdate(List<Repo> repos) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        String username = Setting.getUsername(ClientApp.getContext());

        for (Repo repo : repos) {
            Like obj = Like.forRepoTokenAndUsername(repo.getToken(), username);
            if (obj == null) {
                Like.create(realm, repo.getToken(), username);
            }
        }

        realm.commitTransaction();
    }

    public static Like create(Realm realm, String repoToken, String username) {
        Like like = realm.createObject(Like.class);
        like.setRepoToken(repoToken);
        like.setUsername(username);
        return like;
    }

    public void setRepoToken(String repoToken) {
        this.repoToken = repoToken;
    }

    public String getRepoToken() {
        return this.repoToken;
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
