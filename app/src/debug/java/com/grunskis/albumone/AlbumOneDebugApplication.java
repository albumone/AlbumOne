package com.grunskis.albumone;

import android.app.Application;

import com.facebook.stetho.Stetho;

import timber.log.Timber;

public class AlbumOneDebugApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);

        Timber.plant(new Timber.DebugTree());
    }

}
