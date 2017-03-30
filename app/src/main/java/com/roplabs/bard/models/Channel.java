package com.roplabs.bard.models;

import android.text.TextUtils;
        import com.roplabs.bard.ClientApp;
        import io.realm.Realm;
        import io.realm.RealmObject;
        import io.realm.RealmResults;
        import io.realm.Sort;
        import io.realm.annotations.Ignore;

        import java.util.*;

public class Channel extends RealmObject {

    private String token;
    private String name;
    private String description;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Channel(){

    }

    public Channel(String token, String name, String description, Date createdAt){
        this.token = token;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static int getCount() {
        Realm realm = Realm.getDefaultInstance();
        return (int) realm.where(Channel.class).count();
    }

    public static RealmResults<Channel> forUsername(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Channel> results = realm.where(Channel.class)
                .equalTo("username", username)
                .findAllSorted("createdAt", Sort.DESCENDING);
        return results;
    }

    public static Channel forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Channel.class).equalTo("token", token).findFirst();
    }

    public static Channel create(String token, String name, String description, Date createdAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Channel channel = realm.createObject(Channel.class);

        channel.setToken(token);
        channel.setName(name);
        channel.setDescription(description);
        channel.setUsername(Setting.getUsername(ClientApp.getContext()));

        realm.commitTransaction();

        return channel;
    }

    public void delete() {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();
        this.deleteFromRealm();
        realm.commitTransaction();
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
