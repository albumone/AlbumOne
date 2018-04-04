package com.grunskis.albumone.albums;

import android.os.Bundle;

import com.grunskis.albumone.BuildConfig;

public class UnsplashAlbumsActivity extends RemoteAlbumsActivity {
    private static final String BACKEND_NAME = "Unsplash";
    private static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN_UNSPLASH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: 4/4/2018 implement oauth
        String authToken = getAuthToken(this);
        if (authToken == null) {
            authToken = BuildConfig.UNSPLASH_CLIENT_ID;
        }
        setResultAndFinish(authToken);
    }

    @Override
    protected String getAuthTokenPreferenceKey() {
        return PREF_AUTH_TOKEN;
    }
}
