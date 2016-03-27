package com.sandbox.myfirstapp.app.api;

import android.util.Log;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sandbox.myfirstapp.app.events.IndexFetchEvent;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import com.sandbox.myfirstapp.app.models.Index;
import com.sandbox.myfirstapp.app.models.Repo;
import com.sandbox.myfirstapp.app.models.Setting;
import com.sandbox.myfirstapp.app.models.VideoDownloader;
import io.realm.RealmObject;
import okhttp3.ResponseBody;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

import java.io.IOException;
import java.util.List;

interface MadchatService {
    @GET("query")
    Call<Repo> query( @Query("text") String text, @Query("bundle_token") String bundleToken);

    @GET("bundles")
    Call<List<Index>> listIndex();
}

public class MadchatClient {
    static MadchatService service;
    public static final String BASE_URL = "http://192.168.1.79:3000/";

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

    public static void getQuery(String text, String indexToken) throws IOException {
        Call<Repo> call = getService().query(text, indexToken);
        call.enqueue(new Callback<Repo>() {
            @Override
            public void onResponse(Call<Repo> call, Response<Repo> response) {
                Repo repo = response.body();
                VideoDownloader.downloadVideo(repo.getUrl());
                EventBus.getDefault().post(new VideoQueryEvent(repo.getToken(), repo.getUrl(),repo.getWordList(),repo.getError()));
            }

            @Override
            public void onFailure(Call<Repo> call, Throwable throwable) {
                Log.d("MyActivity", "failure on getQuery ");
                EventBus.getDefault().post(new VideoQueryEvent(null,null,null,"Timeout"));
            }
        });
    }

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


