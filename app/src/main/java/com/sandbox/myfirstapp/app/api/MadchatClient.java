package com.sandbox.myfirstapp.app.api;

import android.util.Log;
import com.sandbox.myfirstapp.app.events.VideoQueryEvent;
import com.sandbox.myfirstapp.app.models.Repo;
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

interface MadchatService {
    @GET("query")
    Call<Repo> query( @Query("text") String text);
}

public class MadchatClient {
    static MadchatService service;
    public static final String BASE_URL = "http://192.168.1.77:3000/";

    public static void getQuery(String text) throws IOException {
        Call<Repo> call = getService().query(text);
        call.enqueue(new Callback<Repo>() {
            @Override
            public void onResponse(Call<Repo> call, Response<Repo> response) {
                Repo repo = response.body();
                EventBus.getDefault().post(new VideoQueryEvent(repo.getUrl(),repo.getError()));
            }

            @Override
            public void onFailure(Call<Repo> call, Throwable throwable) {
                Log.d("MyActivity", "failure on getQuery ");
                EventBus.getDefault().post(new VideoQueryEvent(null,"timeout"));
            }
        });
    }

    private static MadchatService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(MadchatService.class);
        }

        return service;
    }
}


