package com.grunskis.albumone.albums;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

abstract public class RemoteAlbumsActivity extends AppCompatActivity {
    public static final String KEY_AUTH_TOKEN = "KEY_AUTH_TOKEN";

    protected abstract String getAuthTokenPreferenceKey();

    protected String getAuthToken(Context context) {
        String key = getAuthTokenPreferenceKey();
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return mSharedPreferences.getString(key, null);
    }

    protected void setAuthToken(Context context, String authToken) {
        String key = getAuthTokenPreferenceKey();
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mSharedPreferences
                .edit()
                .putString(key, authToken)
                .apply();
    }


    protected void setResultAndFinish(String authToken) {
        Intent result = new Intent();
        result.putExtra(KEY_AUTH_TOKEN, authToken);
        setResult(RESULT_OK, result);
        finish();
    }
}
