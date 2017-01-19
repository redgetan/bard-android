package com.roplabs.bard.api;

import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.User;
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

    @GET("bundles")
    Call<List<Character>> listCharacters();

    @GET("scenes")
    Call<List<Scene>> listScenes(@QueryMap Map<String, String> options);

    @GET("scenes/{sceneToken}")
    Call<Scene> getScene(@Path("sceneToken") String sceneToken);

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
}

