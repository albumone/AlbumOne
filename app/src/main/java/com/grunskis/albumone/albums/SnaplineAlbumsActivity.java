package com.grunskis.albumone.albums;

import android.content.Intent;
import android.os.Bundle;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.source.remote.SnaplineDataSource;

public class SnaplineAlbumsActivity extends RemoteAlbumsActivity {
    public static final int REQUEST_LOGIN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.backend_snapline));

        mRemoteDataSource = SnaplineDataSource.getInstance(this);
        if (mRemoteDataSource.isAuthenticated()) {
            loadAlbums(savedInstanceState == null);
        } else {
            authenticate();
        }
    }

    private void authenticate() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent result) {
        switch (requestCode) {
            case REQUEST_LOGIN:
                if (resultCode == RESULT_OK) {
                    String authToken = result.getDataString();
                    mRemoteDataSource.setAuthToken(authToken);
                    loadAlbums(true);
                } else {
                    finish();
                }
                break;
        }
    }
}

