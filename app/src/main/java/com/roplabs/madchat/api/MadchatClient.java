package com.roplabs.madchat.api;

import android.util.Log;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roplabs.madchat.events.IndexFetchEvent;
import com.roplabs.madchat.events.VideoQueryEvent;
import com.roplabs.madchat.models.Index;
import com.roplabs.madchat.models.Repo;
import com.roplabs.madchat.models.Segment;
import com.roplabs.madchat.models.VideoDownloader;
import io.realm.RealmObject;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.List;

interface MadchatService {
    @GET("query")
    Call<List<Segment>> query(@Query("text") String text, @Query("bundle_token") String bundleToken);

    @GET("bundles")
    Call<List<Index>> listIndex();
}

public class MadchatClient {
    static MadchatService service;
    public static final String BASE_URL = "http://madchat.z9kt2x3bxp.us-west-2.elasticbeanstalk.com";
    private static final OkHttpClient client = new OkHttpClient();


    public static void getIndexList() throws IOException {
        Call<List<Index>> call = getService().listIndex();
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
        Call<List<Segment>> call = getService().query(text, indexToken);
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
                Log.d("MimicActivity", "failure on getQuery ");
                EventBus.getDefault().post(new VideoQueryEvent(null, "timeout"));
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

    private static MadchatService getService() {
        if (service == null) {
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
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            service = retrofit.create(MadchatService.class);
        }

        return service;
    }
}


