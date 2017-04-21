package com.roplabs.bard.models;

import android.text.TextUtils;
import com.roplabs.bard.ClientApp;
import io.realm.*;
import io.realm.annotations.Ignore;

import java.util.*;

public class Repo extends RealmObject {

    private String token;
    private String url;
    private String sourceUrl;
    private String uuid;
    private String filePath;
    private String username;
    private Boolean isPublished;
    private String characterToken;
    private String sceneToken;
    private String thumbnailUrl;
    private String wordList;
    private String error;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Repo(){

    }

    public Repo(String url, String filePath, String wordList, Date createdAt){
        this.url = url;
        this.filePath = filePath;
        this.wordList = wordList;
        this.createdAt = createdAt;
    }

    public static RealmResults<Repo> forUsername(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Repo> results = realm.where(Repo.class)
                                          .equalTo("username", username)
                                          .findAllSorted("createdAt", Sort.DESCENDING);
        return results;
    }

    public static RealmResults<Repo> likesForUsername(String username) {
        Realm realm = Realm.getDefaultInstance();


        // HACK: this will return empty query by default
        RealmQuery<Repo> query = realm.where(Repo.class).equalTo("token", "99999999999999999999999999999999999");

        RealmResults<Like> userLikes = Like.forUsername(username);
        for (Like like : userLikes) {
            query = query.or().equalTo("token", like.getRepoToken());
        }

        return query.findAll();
    }

    public static int getCount() {
        Realm realm = Realm.getDefaultInstance();
        return (int) realm.where(Repo.class).count();
    }

    public static Repo forToken(String token) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Repo.class).equalTo("token", token).findFirst();
    }

    public static Repo create(String token, String repoUrl, String uuid, String characterToken, String sceneToken,
                              String videoPath, String wordList, Date createdAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Repo repo = realm.createObject(Repo.class);

        repo.setToken(token);
        repo.setUsername(Setting.getUsername(ClientApp.getContext()));
        repo.setCharacterToken(characterToken);
        repo.setSceneToken(sceneToken);
        repo.setUrl(repoUrl);
        repo.setUUID(uuid);
        repo.setFilePath(videoPath);
        repo.setWordList(wordList);
        repo.setCreatedAt(createdAt);

        realm.commitTransaction();

        return repo;
    }

    public static Repo createFromOtherUser(String token, String repoUrl, String uuid, String characterToken, String sceneToken,
                              String videoPath, String wordList, Date createdAt, String username, String thumbnailUrl, String sourceUrl) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Repo repo = realm.createObject(Repo.class);

        repo.setToken(token);
        repo.setUsername(username);
        repo.setCharacterToken(characterToken);
        repo.setSceneToken(sceneToken);
        repo.setSourceUrl(sourceUrl);
        repo.setThumbnailUrl(thumbnailUrl);
        repo.setUrl(repoUrl);
        repo.setUUID(uuid);
        repo.setFilePath(videoPath);
        repo.setWordList(wordList);
        repo.setCreatedAt(createdAt);

        realm.commitTransaction();

        return repo;
    }

    public void setTokenAndUrl(String token, String repoUrl) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        this.setToken(token);
        this.setUrl(repoUrl);
        this.setIsPublished(true);

        realm.commitTransaction();
    }

    public void delete() {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();
        this.deleteFromRealm();
        realm.commitTransaction();
    }

    public String title() {
        List<String> phrase = new ArrayList<String>();

        String repoTitle = getWordList();
        if (repoTitle == null) return "";

        String[] wordTagStrings = repoTitle.split(",");
        for (String wordTagString : wordTagStrings) {
           String word = wordTagString.split(":")[0];
            phrase.add(word);
        }

        return TextUtils.join(" ",phrase);
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        if (filePath == null) return "";
        return filePath;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Boolean getIsPublished() {
        if (isPublished == null) return false;
        return isPublished;
    }

    public void setCharacterToken(String characterToken) {
        this.characterToken = characterToken;
    }

    public String getCharacterToken() {
        return characterToken;
    }

    public void setSceneToken(String sceneToken) {
        this.sceneToken = sceneToken;
    }

    public String getSceneToken() {
        return sceneToken;
    }

    /**
     *
     * @return
     * The url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url
     * The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     *
     * @return createdAt
     * The url
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     *
     * @param createdAt
     * The url
     */
    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    /**
     *
     * @return
     * The error
     */
    public String getError() {
        return error;
    }

    /**
     *
     * @param error
     * The error
     */
    public void setError(String error) {
        this.error = error;
    }
    /**
     *
     * @return
     * The wordList
     */
    public String getWordList() {
        return wordList;
    }

    /**
     *
     * @param wordList
     * The word_list
     */
    public void setWordList(String wordList) {
        this.wordList = wordList;
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


    public String getUUID() {
        return this.uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public String getSourceUrl() {
        return this.sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {

        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean equals(Object o){
        if(o instanceof Repo){
            Repo toCompare = (Repo) o;
            return this.getToken().equals(toCompare.getToken());
        }
        return false;
    }

}
