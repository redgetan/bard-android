package com.roplabs.bard;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import com.crashlytics.android.Crashlytics;
import com.instabug.library.IBGInvocationEvent;
import com.instabug.library.Instabug;
import com.roplabs.bard.db.DBMigration;
import com.roplabs.bard.models.AmazonCognito;
import com.roplabs.bard.util.CrashReporter;
import com.squareup.leakcanary.LeakCanary;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;


public class ClientApp extends MultiDexApplication {
    private static ClientApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LeakCanary.install(this);

        Realm.init(this);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(25)
                .migration(new DBMigration())
                .build();

        Realm realm = Realm.getInstance(config);
        Realm.setDefaultConfiguration(config);

        AmazonCognito.init(this);
        Fabric.with(this, new Crashlytics());
//        CrashReporter.init(this);
    }

    public static ClientApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }
}
