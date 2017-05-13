package com.roplabs.bard.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
        import com.roplabs.bard.ClientApp;
import com.roplabs.bard.api.GsonUTCDateAdapter;
import io.realm.Realm;
        import io.realm.RealmObject;
        import io.realm.RealmResults;
        import io.realm.Sort;
        import io.realm.annotations.Ignore;

        import java.util.*;

public class Channel implements Comparable<Channel>, Parcelable {

    private String token;
    private String name;
    private String description;
    private String lastMessage;
    private String mode;
    private String participants;
    private String username;
    private Boolean joined;
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

//    public static int getCount() {
//        Realm realm = Realm.getDefaultInstance();
//        return (int) realm.where(Channel.class).count();
//    }
//
//    public static RealmResults<Channel> forUsername(String username) {
//        Realm realm = Realm.getDefaultInstance();
//        RealmResults<Channel> results = realm.where(Channel.class)
//                .equalTo("username", username)
//                .equalTo("joined", true)
//                .findAllSorted("updatedAt", Sort.DESCENDING);
//        return results;
//    }
//
//    public static Channel forToken(String token) {
//        Realm realm = Realm.getDefaultInstance();
//        return realm.where(Channel.class).equalTo("token", token).findFirst();
//    }
//
//    public static Channel forParticipants(String participants) {
//        Realm realm = Realm.getDefaultInstance();
//        return realm.where(Channel.class).equalTo("participants", participants).findFirst();
//    }

    public static final Creator<Channel> CREATOR = new Creator<Channel>() {
        @Override
        public Channel createFromParcel(Parcel in) {
            return new Channel(in);
        }

        @Override
        public Channel[] newArray(int size) {
            return new Channel[size];
        }
    };

    public static Channel create(Channel remoteChannel) {
        return create(remoteChannel.getToken(),
                remoteChannel.getName(),
                remoteChannel.getDescription(),
                remoteChannel.getMode(),
                remoteChannel.getParticipants(),
                remoteChannel.getCreatedAt(),
                remoteChannel.getUpdatedAt(),
                "");
    }
//
//    public void leave() {
//        Realm realm = Realm.getDefaultInstance();
//        realm.beginTransaction();
//
//        setJoined(false);
//
//        realm.commitTransaction();
//    }
//
    public static Channel create(String token, String name, String description, String mode, String participants, Date createdAt, Date updatedAt, String lastMessage) {
        Channel channel = new Channel();

        channel.setToken(token);
        channel.setName(name);
        channel.setDescription(description);
        channel.setMode(mode);
        channel.setParticipants(participants);
        channel.setUpdatedAt(updatedAt);
        channel.setLastMessage(lastMessage);
        channel.setJoined(true);

        return channel;
    }
//
//    public void delete() {
//        Realm realm = Realm.getDefaultInstance();
//
//        realm.beginTransaction();
//        this.deleteFromRealm();
//        realm.commitTransaction();
//    }

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

    public Boolean getJoined() {
        return this.joined;
    }

    public void setJoined(Boolean joined) {
        this.joined = joined;
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

    public String getMode() {
        if (this.mode == null) return "";
        return this.mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getParticipants() {
        return this.participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        if (this.lastMessage == null) return "";
        return this.lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
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

//    public static void createOrUpdate(List<Channel> channelList) {
//
//        for (Channel channel : channelList) {
//            Channel obj = Channel.forToken(channel.getToken());
//            if (obj == null) {
//                Channel.create(channel);
//            } else {
//                Realm realm = Realm.getDefaultInstance();
//                realm.beginTransaction();
//
//                obj.setName(channel.getName());
//                obj.setDescription(channel.getDescription());
//
//                realm.commitTransaction();
//            }
//        }
//    }

    public static Channel createFromFirebase(String channelToken, HashMap<String, Object> channelResult) {
        String lastMessage = (String) channelResult.get("lastMessage");
        String name = (String) channelResult.get("name");
        String mode = (String) channelResult.get("mode");
        Date updatedAt = new Date(((Long) (channelResult.get("updatedAt")) * 1000));
        Date createdAt = new Date(((Long) (channelResult.get("createdAt")) * 1000));
        HashMap<String, Object> participants = (HashMap<String, Object>) channelResult.get("participants");

        List<String> participantNames = new ArrayList<String>();
        for (String participantName : participants.keySet()) {
            participantNames.add(participantName);
        }


        return create(channelToken, name, "", mode, TextUtils.join(",", participantNames), createdAt, updatedAt, lastMessage);
    }

    public void updateFromFirebase(HashMap<String, Object> channelResult) {
        Date updatedAt = new Date(((Long) (channelResult.get("updatedAt")) * 1000));
        String lastMessage = (String) channelResult.get("lastMessage");

        // update channel info
        this.setUpdatedAt(updatedAt);
        this.setLastMessage(lastMessage);
    }


    public boolean equals(Object o){
        if(o instanceof Channel){
            Channel toCompare = (Channel) o;
            return this.getToken().equals(toCompare.getToken());
        }
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        if (o.getUpdatedAt() == null) return 0;
        if (this.getUpdatedAt() == null) return 0;

        return o.getUpdatedAt().compareTo(this.getUpdatedAt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(token);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(lastMessage);
        dest.writeString(mode);
        dest.writeString(participants);
        dest.writeString(username);
    }

    private Channel(Parcel in) {

        token = in.readString();
        name = in.readString();
        description = in.readString();
        lastMessage = in.readString();
        mode = in.readString();
        participants = in.readString();
        username = in.readString();
    }
}
