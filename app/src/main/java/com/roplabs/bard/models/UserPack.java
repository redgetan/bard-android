package com.roplabs.bard.models;

import io.realm.*;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class UserPack extends RealmObject {

    private String packToken;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UserPack(){

    }

    public UserPack(String packToken, String username){
        this.packToken = packToken;
        this.username = username;
    }

    public static RealmResults<Character> packsForUser(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<UserPack> userPacks = realm.where(UserPack.class).equalTo("username", username).findAll();
        String[] userPackTokens = new String[userPacks.size()];
        for (int i = 0; i < userPacks.size(); i++) {
            userPackTokens[i] = userPacks.get(i).getPackToken();
        }

        if (userPacks.isEmpty()) {
            // invalid token to return empty result
            return realm.where(Character.class).equalTo("token","999999999999999999999").findAll();
        } else {
            return realm.where(Character.class).in("token",userPackTokens).findAll();
        }
    }

    public static UserPack forPackTokenAndUsername(String packToken, String username) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(UserPack.class)
                    .equalTo("packToken", packToken)
                    .equalTo("username", username)
                    .findFirst();
    }

    public static void create(Realm realm, String packToken, String username) {
        UserPack userPack = realm.createObject(UserPack.class);
        userPack.setPackToken(packToken);
        userPack.setUsername(username);
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public String getPackToken() {
        return this.packToken;
    }

    public void setPackToken(String packToken) {
        this.packToken = packToken;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
