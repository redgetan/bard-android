package com.roplabs.bard.models;

import com.roplabs.bard.ClientApp;
import com.roplabs.bard.util.Storage;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post extends RealmObject {

    private int id;
    private String repoSourceUrl;
    private String repoTitle;
    private String repoToken;
    private String username;
    private Date createdAt;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Post(){

    }

    public Post(int id, String repoTitle, String repoSourceUrl, Date createdAt){
        this.id = id;
        this.repoTitle = repoTitle;
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
        create(remotePost.getId(), remotePost.getRepoTitle(), remotePost.getRepoSourceUrl(), remotePost.getCreatedAt());
    }

    public static Post create(int id, String name, String repoSourceUrl, Date createdAt) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        Post post = realm.createObject(Post.class);

        post.setId(id);
        post.setRepoTitle(name);
        post.setRepoSourceUrl(repoSourceUrl);
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

    public String getRepoTitle() {
        return this.repoTitle;
    }

    public void setRepoTitle(String repoTitle) {
        this.repoTitle = repoTitle;
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


    public static void createOrUpdate(List<Post> postList) {

        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        for (Post post : postList) {
            Post obj = Post.forId(post.getId());
            if (obj == null) {
                Post.create(post);
            } else {
                obj.setRepoTitle(post.getRepoTitle());
                obj.setRepoSourceUrl(post.getRepoSourceUrl());
            }
        }
        realm.commitTransaction();
    }

    public String getCacheKey() {
        return "post" + String.valueOf(getId());
    }

    public String getCachedVideoFilePath() {
        return Storage.getCachedVideoFilePath(getCacheKey());
    }
}
