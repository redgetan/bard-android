package com.roplabs.madchat.api;

import android.util.Log;
import com.google.gson.*;
import com.roplabs.madchat.ClientApp;
import com.roplabs.madchat.R;
import com.roplabs.madchat.events.IndexFetchEvent;
import com.roplabs.madchat.events.LoginEvent;
import com.roplabs.madchat.events.SignUpEvent;
import com.roplabs.madchat.events.VideoQueryEvent;
import com.roplabs.madchat.models.*;
import com.roplabs.madchat.ui.MainActivity;
import com.roplabs.madchat.util.Helper;
import io.realm.RealmObject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.List;

interface MadchatService {
    @GET("query")
    Call<List<Segment>> query(@Query("text") String text, @Query("bundle_token") String bundleToken);

    @GET("bundles")
    Call<List<Index>> listIndex();
}

interface AccountService {
    @POST("users/sign_in")
    Call<User> login(@Body User user);

    @POST("users")
    Call<User> signUp(@Body User user);
}

public class MadchatClient {
    static MadchatService codecService;
    static AccountService accountService;

    public static final String CODEC_BASE_URL = "http://madchat.z9kt2x3bxp.us-west-2.elasticbeanstalk.com";
    public static final String ACCOUNT_BASE_URL = "http://a389bcc3.ngrok.io";
    private static final OkHttpClient client = new OkHttpClient();


    public static void getIndexList() throws IOException {
        Call<List<Index>> call = getCodecService().listIndex();
        call.enqueue(new Callback<List<Index>>() {
            @Override
            public void onResponse(Call<List<Index>> call, Response<List<Index>> response) {
                List<Index> indexList = response.body();
                EventBus.getDefault().post(new IndexFetchEvent(indexList, null));
            }

            @Override
            public void onFailure(Call<List<Index>> call, Throwable t) {
                EventBus.getDefault().post(new IndexFetchEvent(null, "failure"));
            }
        });
    }

    public static void getQuery(final String text, String indexToken) throws IOException {
        Call<List<Segment>> call = getCodecService().query(text, indexToken);
        call.enqueue(new Callback<List<Segment>>() {
            @Override
            public void onResponse(Call<List<Segment>> call, Response<List<Segment>> response) {
                List<Segment> segments = response.body();
                if(!response.isSuccess()){
                    String msg = response.raw().message() + " - " + text;
                    EventBus.getDefault().post(new VideoQueryEvent(null, msg));
                } else {
                    EventBus.getDefault().post(new VideoQueryEvent(segments, null));
                }
            }

            @Override
            public void onFailure(Call<List<Segment>> call, Throwable throwable) {
                Log.d("Mimic", "failure on getQuery ");
                EventBus.getDefault().post(new VideoQueryEvent(null, "timeout"));
            }
        });
    }

    public static void doLoginIn(String email, String password) {
        Call<User> call = getAccountService().login(new User(email, password));
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
        Call<User> call = getAccountService().signUp(new User(username, email, password));
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

//    public static void fetchSegments() {
//        Request request = new Request.Builder()
//                .url("http://d22z4oll34c07f.cloudfront.net/segments/70gme6lL86o/hey_18965_37.mp4")
//                .build();
//
//        client.newCall(request).enqueue(new okhttp3.Callback() {
//            @Override
//            public void onFailure(okhttp3.Call call, IOException e) {
//                Log.d("Madchat", "failure on fetchSegments ");
//            }
//
//            @Override
//            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
//                Headers responseHeaders = response.headers();
//                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
//                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
//                }
//            }
//        });
//
//    }

    private static MadchatService getCodecService() {
        if (codecService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(CODEC_BASE_URL)
                    .addConverterFactory(getGsonConverterFactory())
                    .client(getHTTPClient(true))
                    .build();
            codecService = retrofit.create(MadchatService.class);
        }

        return codecService;
    }

    private static AccountService getAccountService() {
        if (accountService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(ACCOUNT_BASE_URL)
                    .addConverterFactory(getGsonConverterFactory())
                    .client(getHTTPClient(false))
                    .build();
            accountService = retrofit.create(AccountService.class);
        }

        return accountService;
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


