package com.grunskis.albumone.albums;

import android.os.Bundle;

import com.grunskis.albumone.BuildConfig;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

// TODO: 3/19/2018 load all collections/photos not just first 10
public class UnsplashAlbumsActivity extends RemoteAlbumsActivity {
    private static final String BACKEND_NAME = "Unsplash";
    private static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN_UNSPLASH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(BACKEND_NAME);

        UnsplashDataSource dataSource = UnsplashDataSource.getInstance();
        //Context context = UnsplashAlbumsActivity.this;
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//        String unsplashAuthToken = preferences.getString(BACKEND_AUTH_TOKEN, null);
        String authToken = getAuthToken(this);
        if (authToken != null) {
            dataSource.setAuthToken(authToken);
        } else {
            dataSource.setAuthToken(BuildConfig.UNSPLASH_CLIENT_ID);
        }
        createPresenter(dataSource);
    }

    @Override
    protected String getAuthTokenPreferenceKey() {
        return PREF_AUTH_TOKEN;
    }
}
