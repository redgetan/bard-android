package com.roplabs.bard;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import com.instabug.library.IBGInvocationEvent;
import com.instabug.library.Instabug;
import com.roplabs.bard.db.DBMigration;
import com.roplabs.bard.models.AmazonCognito;
import com.squareup.leakcanary.LeakCanary;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import io.realm.Realm;
import io.realm.RealmConfiguration;


public class ClientApp extends MultiDexApplication {
    private Tracker mTracker;
    private static ClientApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LeakCanary.install(this);
        RealmConfiguration config = new RealmConfiguration.Builder(this)
                .schemaVersion(2)
                .migration(new DBMigration())
                .build();

        Realm realm = Realm.getInstance(config);
        Realm.setDefaultConfiguration(config);

        AmazonCognito.init(this);

        new Instabug.Builder(this, "aa977106b63d2bcb32d9e9c1319d9142")
                .setInvocationEvent(IBGInvocationEvent.IBGInvocationEventNone)
                .build();
    }

    public static ClientApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }
}
