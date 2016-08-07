package com.roplabs.bard.api;

import com.google.gson.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.events.IndexFetchEvent;
import com.roplabs.bard.events.LoginEvent;
import com.roplabs.bard.events.SignUpEvent;
import com.roplabs.bard.models.*;
import com.roplabs.bard.models.Character;
import com.roplabs.bard.util.Helper;
import io.realm.RealmObject;
import okhttp3.*;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.io.IOException;
import java.util.List;

public class BardClient {
    static BardService  bardService;

    public static final String BASE_URL = "http://localhost:3000";
    private static final OkHttpClient client = new OkHttpClient();


    public static void doLoginIn(String email, String password) {
        Call<User> call = getBardService().login(new User(email, password));
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccess()) {
                    String error = Helper.parseError(response);
                    EventBus.getDefault().post(new LoginEvent(null,error));
                } else {
                    User user = response.body();
                    EventBus.getDefault().post(new LoginEvent(user,null));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                EventBus.getDefault().post(new LoginEvent(null,"timeout"));
            }
        });
    }

    public static void doSignUp(String username, String email, String password) {
        Call<User> call = getBardService().signUp(new User(username, email, password));
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccess()) {
                    String error = Helper.parseError(response);
                    EventBus.getDefault().post(new SignUpEvent(null,error));
                } else {
                    User user = response.body();
                    EventBus.getDefault().post(new SignUpEvent(user,null));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                EventBus.getDefault().post(new SignUpEvent(null,"timeout"));
            }
        });
    }

    public static BardService  getBardService() {
        if (bardService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(getGsonConverterFactory())
                    .client(getHTTPClient(true))
                    .build();
            bardService = retrofit.create(BardService .class);
        }

        return bardService;
    }

    private static OkHttpClient getHTTPClient(final Boolean isAuthenticated) {
        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                Request.Builder builder = chain.request().newBuilder();
                builder.addHeader("Accept", "application/json");

                if (isAuthenticated) {
                    builder.addHeader("Authorization", Setting.getAuthenticationToken(ClientApp.getContext()));
                }

                Request newRequest = builder.build();
                return chain.proceed(newRequest);
            }
        };

        // Add the interceptor to OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(interceptor);
        return builder.build();
    }

    private static GsonConverterFactory getGsonConverterFactory() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

        return GsonConverterFactory.create(gson);
    }
}


