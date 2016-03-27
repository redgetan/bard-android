package com.sandbox.myfirstapp.app;

import android.app.Application;
import io.realm.Realm;
import io.realm.RealmConfiguration;


public class ClientApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }
}
