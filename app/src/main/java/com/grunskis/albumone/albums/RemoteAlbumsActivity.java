package com.grunskis.albumone.albums;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.analytics.Tracker;
import com.grunskis.albumone.AlbumOneApplication;
import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.DbHelper;

import org.parceler.Parcels;

import java.util.List;

import timber.log.Timber;

abstract public class RemoteAlbumsActivity
        extends AppCompatActivity
        implements AlbumsClickListener, Callbacks.GetAlbumsCallback {

    private static final String BUNDLE_ALBUMS = "BUNDLE_ALBUMS";
    private static final String BUNDLE_RVSTATE = "BUNDLE_RVSTATE";

    protected RemoteDataSource mRemoteDataSource;
    protected Tracker mAnalyticsTracker;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private RecyclerView mRecyclerView;
    private ProgressBar mLoading;
    private AlbumsAdapter mAlbumsAdapter;

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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            List<Album> albums = Parcels.unwrap(savedInstanceState.getParcelable(BUNDLE_ALBUMS));
            mAlbumsAdapter.addAlbums(albums);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(BUNDLE_RVSTATE));
        } else {
            loadAlbums(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(BUNDLE_RVSTATE,
                mRecyclerView.getLayoutManager().onSaveInstanceState());
        outState.putParcelable(BUNDLE_ALBUMS, Parcels.wrap(mAlbumsAdapter.getAlbums()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_albums_remote);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AlbumOneApplication application = (AlbumOneApplication) getApplication();
        mAnalyticsTracker = application.getDefaultTracker();

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

        mLoading = findViewById(R.id.pb_loading);

        mRecyclerView = findViewById(R.id.rv_albums);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Timber.d("onLoadMore() page: %d totalItems: %d", page, totalItemsCount);
                getAlbums(page + 1, RemoteAlbumsActivity.this);
            }
        });

        mAlbumsAdapter = new AlbumsAdapter(this, this);
        mRecyclerView.setAdapter(mAlbumsAdapter);
    }

    protected void loadAlbums(boolean showLoadingIndicator) {
        setLoadingIndicator(showLoadingIndicator);
        getAlbums(1, this);
    }

    @Override
    public void onAlbumClick(Album album) {
        showAlbumDetails(album);
    }

    private void showAlbumDetails(Album album) {
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(AlbumDetailActivity.EXTRA_LOCAL_ONLY, false);
        startActivity(intent);
    }

    private void setLoadingIndicator(boolean active) {
        if (active) {
            mLoading.setVisibility(View.VISIBLE);
        } else {
            mLoading.setVisibility(View.INVISIBLE);
        }
    }

    public void getAlbums(final int page, final Callbacks.GetAlbumsCallback callback) {
        mRemoteDataSource.getAlbums(page, callback);
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
        mAlbumsAdapter.addAlbums(albums);
    }

    @Override
    public void onDataNotAvailable() {
        setLoadingIndicator(false);
        // TODO: 4/4/2018 show error message
    }

    public void updateAlbum(Album album) {
        mAlbumsAdapter.updateAlbum(album);
    }

    @Override
    public void onDestroy() {
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }
}
