package com.roplabs.bard.api;

import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.Scene;
import com.roplabs.bard.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.HashMap;
import java.util.List;

public interface BardService {
    @POST("users/sign_in")
    Call<User> login(@Body User user);

    @POST("users")
    Call<User> signUp(@Body User user);

    @GET("bundles")
    Call<List<Character>> listCharacters();

    @GET("bundles/{characterToken}/scenes")
    Call<List<Scene>> listScenes(@Path("characterToken") String characterToken);

    @GET("bundles/{characterToken}/scenes/{sceneToken}/word_list")
    Call<Scene> getSceneWordList(@Path("characterToken") String characterToken, @Path("sceneToken") String sceneToken);

    @GET("bundles/{characterToken}/word_list")
    Call<HashMap<String, String>> getCharacterWordList(@Path("characterToken") String characterToken);

    @POST("repos")
    Call<HashMap<String, String>> postRepo(@Body HashMap<String, String> body);
}

