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
    private String type;
    private String participants;
    private String username;
    private Date updatedAt;
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

    public static Channel create(Channel remoteChannel) {
        return create(remoteChannel.getToken(),
                remoteChannel.getName(),
                remoteChannel.getDescription(),
                remoteChannel.getType(),
                remoteChannel.getParticipants(),
                remoteChannel.getCreatedAt(),
                remoteChannel.getUpdatedAt());
    }

    public static Channel create(String token, String name, String description, String type, String participants, Date createdAt, Date updatedAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Channel channel = realm.createObject(Channel.class);

        channel.setToken(token);
        channel.setName(name);
        channel.setDescription(description);
        channel.setType(type);
        channel.setParticipants(participants);
        channel.setUpdatedAt(updatedAt);
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

    public String getType() {
        if (this.type == null) return "";
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParticipants() {
        return this.participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {

        this.updatedAt = updatedAt;
    }

    public String getReceiver() {
        String[] participants = getParticipants().split(",");
        if (participants.length == 2) {
            String receiver = "";
            for (int i = 0; i < participants.length; i++) {
                if (!participants[i].equals(Setting.getUsername(ClientApp.getContext()))) {
                    receiver = participants[i];
                    break;
                }
            }

            return receiver;
        } else {
            return "";
        }
    }

    public static void createOrUpdate(List<Channel> channelList) {

        for (Channel channel : channelList) {
            Channel obj = Channel.forToken(channel.getToken());
            if (obj == null) {
                Channel.create(channel);
            } else {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();

                obj.setName(channel.getName());
                obj.setDescription(channel.getDescription());

                realm.commitTransaction();
            }
        }
    }
}
