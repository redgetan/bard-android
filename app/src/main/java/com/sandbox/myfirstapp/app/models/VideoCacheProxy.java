package com.sandbox.myfirstapp.app.models;

import android.content.Context;
import com.danikula.videocache.HttpProxyCacheServer;

public class VideoCacheProxy{

    private static HttpProxyCacheServer sharedProxy;

    private VideoCacheProxy() {
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        return sharedProxy == null ? (sharedProxy = newProxy(context)) : sharedProxy;
    }

    private static HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer.Builder(context.getApplicationContext())
                .maxCacheFilesCount(20)
                .build();
    }
}
