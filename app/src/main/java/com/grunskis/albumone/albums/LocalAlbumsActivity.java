package com.grunskis.albumone.albums;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;
import com.grunskis.albumone.data.source.local.DbHelper;
import com.grunskis.albumone.data.source.local.LocalDataSource;
import com.grunskis.albumone.widget.AlbumOneWidget;

import org.parceler.Parcels;

import java.util.List;

public class LocalAlbumsActivity
        extends AppCompatActivity
        implements AlbumsClickListener, Callbacks.GetAlbumsCallback,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String BUNDLE_ALBUMS = "BUNDLE_ALBUMS";
    private static final String BUNDLE_RVSTATE = "BUNDLE_RVSTATE";

    private static final int ALBUMS_LOADER = 1;

    private static final String ACCOUNT = "default";
    private static final String ACCOUNT_TYPE = "com.grunskis.albumone";
    private static final long SYNC_INTERVAL = 60 * 60; // sync every hour

    private int mAppWidgetId;

    private FloatingActionButton mFABAdd;
    private FloatingActionButton mFABUnsplash;
    private FloatingActionButton mFABGooglePhotos;
    private boolean mIsFABOpen;

    private ProgressBar mLoading;

    private RecyclerView mRecyclerView;
    private AlbumsAdapter mAlbumsAdapter;
    private LoaderManager mLoaderManager;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onRestart() {
        super.onRestart();
        mLoaderManager.restartLoader(ALBUMS_LOADER, null, this);
    }

    private static void createSyncAccount(Context context) {
        Account account = new Account(ACCOUNT, ACCOUNT_TYPE);

        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager == null) {
            return;
        }

        if (accountManager.addAccountExplicitly(account, null, null)) {
            final String authority = AlbumOnePersistenceContract.CONTENT_AUTHORITY;

            // setup automatic, periodic sync
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, SYNC_INTERVAL);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_albums);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createSyncAccount(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (isWidgetConfiguration()) {
            // prepare response in case user abandons the configuration activity
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_CANCELED, resultValue);

            setTitle(getString(R.string.choose_album));
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Album album = Parcels.unwrap(
                        intent.getParcelableExtra(DownloadService.EXTRA_ALBUM));
                updateAlbum(album);
            }
        };
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_FINISHED));

        mFABAdd = findViewById(R.id.fab);
        mFABUnsplash = findViewById(R.id.fab_unsplash);
        mFABGooglePhotos = findViewById(R.id.fab_gphotos);
        mIsFABOpen = false;

        mFABAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }
            }
        });

        if (isWidgetConfiguration()) {
            mFABAdd.setVisibility(View.GONE);
            mFABUnsplash.setVisibility(View.GONE);
            mFABGooglePhotos.setVisibility(View.GONE);
        }

        mFABUnsplash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFABMenu();

                Intent intent = new Intent(LocalAlbumsActivity.this,
                        UnsplashAlbumsActivity.class);
                startActivity(intent);
            }
        });

        mFABGooglePhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFABMenu();

                Intent intent = new Intent(LocalAlbumsActivity.this,
                        PicasawebAlbumsActivity.class);
                startActivity(intent);
            }
        });

        mLoading = findViewById(R.id.pb_loading);
        mLoaderManager = getSupportLoaderManager();

        mRecyclerView = findViewById(R.id.rv_albums);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mAlbumsAdapter = new AlbumsAdapter(this, this);
        mRecyclerView.setAdapter(mAlbumsAdapter);

        if (savedInstanceState != null) {
            List<Album> albums = Parcels.unwrap(savedInstanceState.getParcelable(BUNDLE_ALBUMS));
            mAlbumsAdapter.addAlbums(albums);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(BUNDLE_RVSTATE));
        } else {
            setLoadingIndicator(true);
            mLoaderManager.restartLoader(ALBUMS_LOADER, null, this);
        }
    }

    private boolean isWidgetConfiguration() {
        return mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID;
    }

    @Override
    public void onDestroy() {
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }

    private void showFABMenu() {
        mIsFABOpen = true;

        float fabTransY = getResources().getDimension(R.dimen.fab_translation_y);
        mFABUnsplash.animate().translationY(-fabTransY);
        mFABGooglePhotos.animate().translationY(-fabTransY * 2);
        mFABAdd.animate().rotationBy(45);
    }

    private void closeFABMenu() {
        mFABUnsplash.animate().translationY(0);
        mFABGooglePhotos.animate().translationY(0);
        mFABAdd.animate().rotationBy(-45);

        mIsFABOpen = false;
    }

    @Override
    public void onBackPressed() {
        if (mIsFABOpen) {
            closeFABMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAlbumClick(Album album) {
        if (isWidgetConfiguration()) {
            saveWidgetData(album);
        } else {
            showAlbumDetails(album);
        }
    }

    private void saveWidgetData(Album album) {
        AlbumOneWidget.saveWidgetData(this, mAppWidgetId, album.getId());

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        AlbumOneWidget.updateWidget(this, appWidgetManager, mAppWidgetId, album);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void showAlbumDetails(Album album) {
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(AlbumDetailActivity.EXTRA_LOCAL_ONLY, true);
        startActivity(intent);
    }

    @Override
    public void onAlbumsLoaded(List<Album> albums) {
        setLoadingIndicator(false);

        for (Album album : albums) {
            // TODO: 4/10/2018 figure out where 0 comes from..
            if (album.getCoverPhoto().getId() != null && album.getCoverPhoto().getId() > 0) {
                album.getCoverPhoto().refreshFromDb(this);
            }

            Download download = DbHelper.getDownload(this, album.getRemoteId());
            if (download == null) {
                album.setDownloadState(Album.DownloadState.NOT_DOWNLOADED);
            } else {
                if (download.getFinishedAt() == null) {
                    album.setDownloadState(Album.DownloadState.DOWNLOADING);
                } else {
                    album.setDownloadState(Album.DownloadState.DOWNLOADED);
                }
            }
        }

        showAlbums(albums);
    }

    public void showAlbums(List<Album> albums) {
        mAlbumsAdapter.setAlbums(albums);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        outState.putParcelable(BUNDLE_RVSTATE,
                mRecyclerView.getLayoutManager().onSaveInstanceState());
        outState.putParcelable(BUNDLE_ALBUMS, Parcels.wrap(mAlbumsAdapter.getAlbums()));
    }

    public void updateAlbum(Album album) {
        mAlbumsAdapter.updateAlbum(album);
    }

    public void setLoadingIndicator(boolean active) {
        if (active) {
            mLoading.setVisibility(View.VISIBLE);
        } else {
            mLoading.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDataNotAvailable() {
        setLoadingIndicator(false);
        // TODO: 4/4/2018 show error message
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(BUNDLE_RVSTATE,
                mRecyclerView.getLayoutManager().onSaveInstanceState());
        outState.putParcelable(BUNDLE_ALBUMS, Parcels.wrap(mAlbumsAdapter.getAlbums()));
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(
                LocalAlbumsActivity.this,
                AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI,
                AlbumOnePersistenceContract.AlbumEntry.COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            onAlbumsLoaded(LocalDataSource.Albums.from(data));
        } else {
            onDataNotAvailable();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
