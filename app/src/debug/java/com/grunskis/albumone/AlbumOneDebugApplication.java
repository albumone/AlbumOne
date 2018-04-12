package com.grunskis.albumone;

import com.facebook.stetho.Stetho;

import timber.log.Timber;

public class AlbumOneDebugApplication extends AlbumOneApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);

        Timber.plant(new Timber.DebugTree());
    }
}
