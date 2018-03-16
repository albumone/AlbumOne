package com.grunskis.albumone.albums;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.LocalDataSource;

abstract public class RemoteAlbumsActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_albums, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_albums);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.fab).setVisibility(View.GONE);
        findViewById(R.id.fab_unsplash).setVisibility(View.GONE);
        findViewById(R.id.fab_gphotos).setVisibility(View.GONE);
    }

    protected void createPresenter(RemoteDataSource remoteDataSource) {
        LoaderProvider loaderProvider = new LoaderProvider(this);

        AlbumsFragment albumsFragment =
                (AlbumsFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (albumsFragment == null) {
            albumsFragment = AlbumsFragment.newInstance(false);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.content_frame, albumsFragment);
            transaction.commit();
        }

//        AlbumsRepository repository = Injection.provideAlbumsRepository(
//                getApplicationContext(),
//                loaderProvider,
//                getSupportLoaderManager());

        AlbumsRepository repository = new AlbumsRepository(
                remoteDataSource,
                LocalDataSource.getInstance(
                        getApplicationContext().getContentResolver(),
                        loaderProvider,
                        getSupportLoaderManager()));

        new AlbumsPresenter(repository, albumsFragment);
    }

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
}
