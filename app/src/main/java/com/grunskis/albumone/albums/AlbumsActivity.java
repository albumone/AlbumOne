package com.grunskis.albumone.albums;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grunskis.albumone.DisplayHelpers;
import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.GlideApp;
import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.LocalDataSource;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;
import com.grunskis.albumone.widget.AlbumOneWidget;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AlbumsActivity
        extends AppCompatActivity
        implements AlbumsClickListener, Callbacks.GetAlbumsCallback {

    private static final String EXTRA_REMOTE_TYPE =
            "com.grunskis.albumone.albums.EXTRA_REMOTE_TYPE";
    private static final String EXTRA_AUTH_TOKEN =
            "com.grunskis.albumone.albums.EXTRA_AUTH_TOKEN";

    private static final String BUNDLE_ALBUMS = "BUNDLE_ALBUMS";
    private static final String BUNDLE_RVSTATE = "BUNDLE_RVSTATE";

    private static final int REQUEST_AUTH_GOOGLE_PHOTOS = 1;
    private static final int REQUEST_AUTH_UNSPLASH = 2;

    private int mAppWidgetId;
    protected FloatingActionButton mFABAdd;
    protected RecyclerView mRecyclerView;
    protected LinearLayoutManager mLayoutManager;
    protected AlbumsRepository mAlbumsRepository;
    private FloatingActionButton mFABUnsplash;
    private FloatingActionButton mFABGooglePhotos;
    private boolean mIsFABOpen;
    private boolean mShowLocalAlbums;
    private ProgressBar mLoading;
    private AlbumsAdapter mAlbumsAdapter;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mShowLocalAlbums) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_albums, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_AUTH_GOOGLE_PHOTOS:
                if (resultCode == RESULT_OK) {
                    String authToken = data.getStringExtra(PicasawebAlbumsActivity.KEY_AUTH_TOKEN);
                    openRemoteAlbum(RemoteType.GOOGLE_PHOTOS, authToken);
                }
                // TODO: 4/4/2018 show error
                break;

            case REQUEST_AUTH_UNSPLASH:
                if (resultCode == RESULT_OK) {
                    String authToken = data.getStringExtra(UnsplashAlbumsActivity.KEY_AUTH_TOKEN);
                    openRemoteAlbum(RemoteType.UNSPLASH, authToken);
                }
                // TODO: 4/4/2018 show error
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void openRemoteAlbum(RemoteType remoteType, String authToken) {
        Intent intent = new Intent(this, AlbumsActivity.class);
        intent.putExtra(EXTRA_REMOTE_TYPE, remoteType);
        intent.putExtra(EXTRA_AUTH_TOKEN, authToken);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_albums);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        mFABUnsplash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AlbumsActivity.this, UnsplashAlbumsActivity.class);
                startActivityForResult(intent, REQUEST_AUTH_UNSPLASH);
                closeFABMenu();
            }
        });

        mFABGooglePhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AlbumsActivity.this, PicasawebAlbumsActivity.class);
                startActivityForResult(intent, REQUEST_AUTH_GOOGLE_PHOTOS);
                closeFABMenu();
            }
        });

        RemoteType remoteType = (RemoteType) getIntent().getSerializableExtra(EXTRA_REMOTE_TYPE);
        mShowLocalAlbums = remoteType == null;

        if (!mShowLocalAlbums || isWidgetConfiguration()) {
            mFABAdd.setVisibility(View.GONE);
            mFABUnsplash.setVisibility(View.GONE);
            mFABGooglePhotos.setVisibility(View.GONE);
        }

        RemoteDataSource remoteDataSource = null;
        if (remoteType != null) {
            switch (remoteType) {
                case GOOGLE_PHOTOS:
                    remoteDataSource = PicasaWebDataSource.getInstance();
                    setTitle(getResources().getString(R.string.backend_google_photos));
                    break;

                case UNSPLASH:
                    remoteDataSource = UnsplashDataSource.getInstance();
                    setTitle(getResources().getString(R.string.backend_unsplash));
                    break;
            }

            remoteDataSource.setAuthToken(getIntent().getStringExtra(EXTRA_AUTH_TOKEN));
        }

        mAlbumsRepository = new AlbumsRepository(
                remoteDataSource,
                LocalDataSource.getInstance(
                        getApplicationContext().getContentResolver(),
                        new LoaderProvider(this),
                        getSupportLoaderManager()));

        mLoading = findViewById(R.id.pb_loading);

        mRecyclerView = findViewById(R.id.rv_albums);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        if (!mShowLocalAlbums) {
            mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(mLayoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                    Timber.d("onLoadMore() page: %d totalItems: %d", page, totalItemsCount);
                    mAlbumsRepository.getAlbums(page + 1, AlbumsActivity.this);
                }
            });
        }

        mAlbumsAdapter = new AlbumsAdapter(this, this);
        mRecyclerView.setAdapter(mAlbumsAdapter);

        if (savedInstanceState != null) {
            List<Album> albums = Parcels.unwrap(savedInstanceState.getParcelable(BUNDLE_ALBUMS));
            mAlbumsAdapter.addAlbums(albums);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(BUNDLE_RVSTATE));
        } else {
            setLoadingIndicator(true);
            mAlbumsRepository.getAlbums(1, this);
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
        mFABGooglePhotos.animate().translationY(-fabTransY*2);
        mFABAdd.animate().rotationBy(45);
    }

    private void closeFABMenu(){
        mFABUnsplash.animate().translationY(0);
        mFABGooglePhotos.animate().translationY(0);
        mFABAdd.animate().rotationBy(-45);

        mIsFABOpen = false;
    }

    @Override
    public void onBackPressed() {
        if (mIsFABOpen){
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
        intent.putExtra(AlbumDetailActivity.EXTRA_LOCAL_ONLY, mShowLocalAlbums);
        startActivity(intent);
    }

    @Override
    public void onAlbumsLoaded(List<Album> albums) {
        setLoadingIndicator(false);

        for (Album album : albums) {
            if (album.getCoverPhoto().getId() != null) {
                album.getCoverPhoto().refreshFromDb(this);
            }

            Download download = mAlbumsRepository.getDownload(album.getRemoteId());
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

    private static class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {
        private List<Album> mAlbums;
        private AlbumsClickListener mAlbumClickListener;
        private int mDisplayWidth;
        private Context mContext;

        AlbumsAdapter(Context context, AlbumsClickListener albumClickListener) {
            mContext = context;
            mAlbumClickListener = albumClickListener;

            mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        }

        void resetAlbums() {
            if (mAlbums != null && mAlbums.size() > 0) {
                mAlbums.clear();
                notifyDataSetChanged();
            }
        }

        void addAlbums(List<Album> albums) {
            if (mAlbums == null) {
                mAlbums = new ArrayList<>(albums);
            } else {
                mAlbums.addAll(albums);
            }
            notifyDataSetChanged();
        }

        void updateAlbum(Album album) {
            for (int i = 0; i < mAlbums.size(); i++) {
                Album a = mAlbums.get(i);
                if (a != null && a.getId() != null && a.getId().equals(album.getId())) {
                    mAlbums.set(i, album);
                    notifyItemChanged(i);
                    break;
                }
            }
        }

        public List<Album> getAlbums() {
            return mAlbums;
        }

        @NonNull
        @Override
        public AlbumsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_album, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final AlbumsAdapter.ViewHolder holder, int position) {
            final Album album = mAlbums.get(position);
            Photo coverPhoto = album.getCoverPhoto();

            // TODO: 4/4/2018 check recycling

            holder.itemView.getLayoutParams().height = DisplayHelpers.calculateOptimalPhotoHeight(
                    mDisplayWidth, coverPhoto);

            Uri uri;
            String localPath = coverPhoto.getDownloadPath();
            if (localPath != null && localPath.length() > 0) {
                uri = Uri.fromFile(new File(localPath));
            } else {
                uri = coverPhoto.getSmallUri();
            }
            GlideApp.with(mContext).load(uri).into(holder.mCoverPhoto);

            holder.mTitle.setText(album.getTitle());

            if (album.getDownloadState() == Album.DownloadState.DOWNLOADING) {
                holder.mDownloadProgress.setVisibility(View.VISIBLE);
            } else if (album.getDownloadState() == Album.DownloadState.DOWNLOADED) {
                holder.mDownloadProgress.setVisibility(View.INVISIBLE);
            }
            if (album.isLocal()) {
                if (album.getRemoteType() == RemoteType.GOOGLE_PHOTOS) {
                    holder.mBackendLogo.setImageResource(R.drawable.ic_google_photos_24dp);
                } else if (album.getRemoteType() == RemoteType.UNSPLASH) {
                    holder.mBackendLogo.setImageResource(R.drawable.ic_unsplash_24dp);
                }
                holder.mBackendLogo.setVisibility(View.VISIBLE);
            }

            holder.mCoverPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (album.getDownloadState() == Album.DownloadState.DOWNLOADING) {
                        Snackbar.make(view,
                                view.getResources().getString(R.string.album_downloading),
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        mAlbumClickListener.onAlbumClick(album);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mAlbums == null) {
                return 0;
            } else {
                return mAlbums.size();
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView mCoverPhoto;
            final TextView mTitle;
            final ImageView mBackendLogo;
            final ProgressBar mDownloadProgress;

            ViewHolder(View itemView) {
                super(itemView);

                mCoverPhoto = itemView.findViewById(R.id.iv_cover_photo);
                mTitle = itemView.findViewById(R.id.tv_title);
                mBackendLogo = itemView.findViewById(R.id.iv_backend_type);
                mDownloadProgress = itemView.findViewById(R.id.pb_album_downoading);
            }
        }
    }
}
