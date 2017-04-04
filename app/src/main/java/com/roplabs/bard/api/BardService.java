package com.roplabs.bard.api;

import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BardService {
    @POST("users/sign_in")
    Call<User> login(@Body User user);

    @POST("users")
    Call<User> signUp(@Body User user);

    @GET("users/{username}/packs")
    Call<List<Character>> listCharacters(@Path("username") String username);

    @GET("users/{username}/channels")
    Call<List<Channel>> listChannels(@Path("username") String username);

    @GET("scenes")
    Call<List<Scene>> listScenes(@QueryMap Map<String, String> options);

    @GET("scenes/{sceneToken}")
    Call<Scene> getScene(@Path("sceneToken") String sceneToken);

    @POST("scenes/{sceneToken}/favorite")
    Call<HashMap<String, String>> favoriteScene(@Path("sceneToken") String sceneToken);

    @POST("scenes/{sceneToken}/unfavorite")
    Call<HashMap<String, String>> unfavoriteScene(@Path("sceneToken") String sceneToken);

    @POST("repos/{repoToken}/like")
    Call<HashMap<String, String>> likeRepo(@Path("repoToken") String repoToken);

    @POST("repos/{repoToken}/unlike")
    Call<HashMap<String, String>> unlikeRepo(@Path("repoToken") String repoToken);

    @GET("scenes/{sceneToken}/word_list")
    Call<Scene> getSceneWordList(@Path("sceneToken") String sceneToken);

    @GET("bundles/{characterToken}/word_list")
    Call<HashMap<String, String>> getCharacterWordList(@Path("characterToken") String characterToken);

    @POST("repos")
    Call<HashMap<String, String>> postRepo(@Body HashMap<String, String> body);

    @POST("upload")
    Call<HashMap<String, String>> postUploadVideo(@Query("youtube_url") String youtubeUrl);

    @POST("repos/{repoToken}/delete")
    Call<HashMap<String, String>> deleteRepo(@Path("repoToken") String repoToken);

    @POST("packs/{packToken}/favorite")
    Call<HashMap<String, String>> favoritePack(@Path("packToken") String packToken);

    @POST("packs/{packToken}/unfavorite")
    Call<HashMap<String, String>> unfavoritePack(@Path("packToken") String packToken);

    @POST("packs")
    Call<Character> createPack(@Body HashMap<String, String> body);

    @GET("packs/{packToken}")
    Call<Character> getCharacter(@Path("packToken") String packToken);

    @POST("channels")
    Call<Channel> createChannel(@Body HashMap<String, String> body);

    @GET("channels/{channelToken}/posts")
    Call<List<Post>> getChannelPosts(@Path("channelToken") String channelToken, @QueryMap Map<String, String> options);

    @POST("repos/{repoToken}/post_to_channel")
    Call<HashMap<String, String>> postRepoToChannel(@Path("repoToken") String repoToken, @Body HashMap<String, String> body);

}

