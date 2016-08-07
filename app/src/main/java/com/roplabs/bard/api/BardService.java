package com.roplabs.bard.api;

import com.roplabs.bard.models.Character;
import com.roplabs.bard.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.List;

public interface BardService {
    @POST("users/sign_in")
    Call<User> login(@Body User user);

    @POST("users")
    Call<User> signUp(@Body User user);

    @GET("bundles")
    Call<List<Character>> listIndex();
}

