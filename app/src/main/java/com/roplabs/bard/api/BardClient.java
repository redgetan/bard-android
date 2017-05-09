package com.roplabs.bard.api;

import com.google.gson.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.*;
import io.realm.RealmObject;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BardClient {
    static BardService  nonauthenticatedBardService;
    static BardService  authenticatedBardService;
    static BardService  lambdaService;

    private static final OkHttpClient client = new OkHttpClient();

    public static BardService  getAuthenticatedBardService() {
        if (Setting.getAuthenticationToken(ClientApp.getContext()).isEmpty()) {
            return getNonauthenticatedBardService();
        }

        if (authenticatedBardService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Configuration.bardAPIBaseURL())
                    .addConverterFactory(getGsonConverterFactory())
                    .client(getHTTPClient(true))
                    .build();
            authenticatedBardService = retrofit.create(BardService .class);
        }

        return authenticatedBardService;
    }


    public static BardService  getNonauthenticatedBardService() {
        if (nonauthenticatedBardService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Configuration.bardAPIBaseURL())
                    .addConverterFactory(getGsonConverterFactory())
                    .client(getHTTPClient(false))
                    .build();
            nonauthenticatedBardService = retrofit.create(BardService .class);
        }

        return nonauthenticatedBardService;
    }

    public static BardService  getLambdaBardService() {
        if (lambdaService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Configuration.bardLambdaBaseURL())
                    .client(getHTTPClient(Setting.isLogined(ClientApp.getContext())))
                    .build();
            lambdaService = retrofit.create(BardService.class);
        }

        return lambdaService;
    }

    private static OkHttpClient getHTTPClient(final Boolean isAuthenticated) {
        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                Request.Builder builder = chain.request().newBuilder();
                builder.addHeader("Accept", "application/json");

                if (isAuthenticated) {
                    builder.addHeader("Authorization", "Token " + Setting.getAuthenticationToken(ClientApp.getContext()));
                }

                Request newRequest = builder.build();
                return chain.proceed(newRequest);
            }
        };

        // Add the interceptor to OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(20, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(60,TimeUnit.SECONDS);
        builder.interceptors().add(interceptor);
        return builder.build();
    }

    private static GsonConverterFactory getGsonConverterFactory() {
        Gson gson = new GsonBuilder()
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
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .create();

        return GsonConverterFactory.create(gson);
    }


}


