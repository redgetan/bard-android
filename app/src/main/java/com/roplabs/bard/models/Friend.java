package com.roplabs.bard.models;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Friend extends RealmObject {

    private String thumbnailUrl;
    private String friendname;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Friend(){

    }

    public Friend(String friendname, String username){
        this.friendname = friendname;
        this.username = username;
    }

    public static RealmResults<Friend> friendsForUser(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Friend> userFriends = realm.where(Friend.class).equalTo("username", username).findAll();
        return userFriends;
    }

    public static Friend create(Realm realm, String friendname, String username) {
        Friend friend = realm.createObject(Friend.class);
        friend.setFriendname(friendname);
        friend.setUsername(username);
        return friend;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public String getFriendname() {
        return this.friendname;
    }

    public void setFriendname(String friendname) {
        this.friendname = friendname;
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
