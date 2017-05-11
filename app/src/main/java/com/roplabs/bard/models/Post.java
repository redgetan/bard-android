package com.roplabs.bard.models;

import android.text.TextUtils;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.api.GsonUTCDateAdapter;
import com.roplabs.bard.util.Storage;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;

import java.util.*;

public class Post extends RealmObject {

    private int id;
    private String repoSourceUrl;
    private String repoTitle;
    private String repoToken;
    private String sceneToken;
    private String packToken;
    private String thumbnailUrl;
    private String repoWordList;
    private String username;
    private Date createdAt;
    private Date updatedAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Post(){

    }

    public Post(int id, String repoWordList, String repoSourceUrl, Date createdAt){
        this.id = id;
        this.repoWordList = repoWordList;
        this.repoSourceUrl = repoSourceUrl;
        this.createdAt = createdAt;
    }

    public static RealmResults<Post> findAll() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Post.class).findAll();
    }

    public static int getCount() {
        Realm realm = Realm.getDefaultInstance();
        return (int) realm.where(Post.class).count();
    }

    public static RealmResults<Post> forUsername(String username) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Post> results = realm.where(Post.class)
                .equalTo("username", username)
                .findAllSorted("createdAt", Sort.DESCENDING);
        return results;
    }

    public static Post forId(int id) {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Post.class).equalTo("id", id).findFirst();
    }

    public static void create(Post remotePost) {
        create(remotePost.getId(), remotePost.getRepoToken(), remotePost.getRepoWordList(), remotePost.getRepoSourceUrl(), remotePost.getSceneToken(), remotePost.getPackToken(), remotePost.getThumbnailUrl(), remotePost.getCreatedAt());
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("repoSourceUrl", getRepoSourceUrl());
        result.put("repoToken", getRepoToken());
        result.put("sceneToken", getSceneToken());
        result.put("packToken", getPackToken());
        result.put("thumbnailUrl", getThumbnailUrl());
        result.put("repoWordList", getRepoWordList());
        result.put("username", getUsername());
        result.put("createdAt", getCreatedAt().getTime() / 1000);
        result.put("updatedAt", getUpdatedAt().getTime() / 1000);

        return result;
    }

    public static Post fromResult(HashMap<String, String> result) {
        Post post = new Post();
        post.setRepoSourceUrl(result.get("repoSourceUrl"));
        post.setRepoToken(result.get("repoToken"));
        post.setSceneToken(result.get("sceneToken"));
        post.setPackToken(result.get("packToken"));
        post.setThumbnailUrl(result.get("thumbnailUrl"));
        post.setRepoWordList(result.get("repoWordList"));
        post.setUsername(result.get("username"));
        post.setCreatedAt(GsonUTCDateAdapter.parseDate(result.get("createdAt")));
        post.setUpdatedAt(GsonUTCDateAdapter.parseDate(result.get("updatedAt")));

        return post;
    }

    public static Post fromFirebase(HashMap<String, Object> result) {
        Post post = new Post();
        post.setRepoSourceUrl((String) result.get("repoSourceUrl"));
        post.setRepoToken((String) result.get("repoToken"));
        post.setSceneToken((String) result.get("sceneToken"));
        post.setPackToken((String) result.get("packToken"));
        post.setThumbnailUrl((String) result.get("thumbnailUrl"));
        post.setRepoWordList((String) result.get("repoWordList"));
        post.setUsername((String) result.get("username"));
        Date createdAt = new Date((Long) result.get("createdAt") * 1000);
        Date updatedAt = new Date((Long) result.get("updatedAt") * 1000);
        post.setCreatedAt(createdAt);
        post.setUpdatedAt(updatedAt);

        return post;
    }

    public static Post create(int id, String repoToken, String repoWordList, String repoSourceUrl, String sceneToken, String packToken, String thumbnailUrl,  Date createdAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Post post = realm.createObject(Post.class);

        post.setId(id);
        post.setRepoToken(repoToken);
        post.setRepoWordList(repoWordList);
        post.setRepoSourceUrl(repoSourceUrl);
        post.setSceneToken(sceneToken);
        post.setPackToken(packToken);
        post.setThumbnailUrl(thumbnailUrl);
        post.setUsername(Setting.getUsername(ClientApp.getContext()));

        realm.commitTransaction();

        return post;
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

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRepoWordList() {
        return this.repoWordList;
    }

    public void setRepoWordList(String repoWordList) {
        this.repoWordList = repoWordList;
    }

    public String getRepoSourceUrl() {
        return this.repoSourceUrl;
    }

    public void setRepoSourceUrl(String repoSourceUrl) {
        this.repoSourceUrl = repoSourceUrl;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public static void createOrUpdate(List<Post> postList) {

        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        for (Post post : postList) {
            Post obj = Post.forId(post.getId());
            if (obj == null) {
                Post.create(post);
            } else {
                obj.setRepoWordList(post.getRepoWordList());
                obj.setRepoSourceUrl(post.getRepoSourceUrl());
            }
        }
        realm.commitTransaction();
    }

    public String getRepoToken() {
        return repoToken;
    }

    public void setRepoToken(String repoToken) {

        this.repoToken = repoToken;
    }

    public String getSceneToken() {
        return sceneToken;
    }

    public void setSceneToken(String sceneToken) {

        this.sceneToken = sceneToken;
    }

    public String getPackToken() {
        return packToken;
    }

    public void setPackToken(String packToken) {

        this.packToken = packToken;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {

        this.thumbnailUrl = thumbnailUrl;
    }

    public String getCacheKey() {
        return "post" + String.valueOf(getId());
    }

    public String getCachedVideoFilePath() {
        return Storage.getCachedVideoFilePath(getCacheKey());
    }

    public String getTitle() {
        List<String> phrase = new ArrayList<String>();

        String repoTitle = getRepoWordList();
        if (repoTitle == null) return "";

        String[] wordTagStrings = repoTitle.split(",");
        for (String wordTagString : wordTagStrings) {
            String word = wordTagString.split(":")[0];
            phrase.add(word);
        }

        return TextUtils.join(" ",phrase);
    }
}
