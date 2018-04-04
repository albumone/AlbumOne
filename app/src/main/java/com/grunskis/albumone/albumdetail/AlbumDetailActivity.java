package com.grunskis.albumone.albumdetail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
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

import com.grunskis.albumone.DisplayHelpers;
import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.LocalDataSource;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;
import com.grunskis.albumone.gallery.GalleryActivity;
import com.grunskis.albumone.util.StethoUtil;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class AlbumDetailActivity
        extends AppCompatActivity
        implements PhotoClickListener, Callbacks.GetAlbumPhotosCallback {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.albumdetail.EXTRA_ALBUM";
    public static final String EXTRA_LOCAL_ONLY =
            "com.grunskis.albumone.albumdetail.EXTRA_LOCAL_ONLY";

    private static final String BUNLDE_PHOTOS = "BUNDLE_PHOTOS";
    private static final String BUNLDE_RVSTATE = "BUNDLE_RVSTATE";

    private Album mAlbum;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressBar mDownloadProgressBar;
    private ProgressBar mLoading;
    private RecyclerView mRecyclerView;
    private PhotosAdapter mPhotosAdapter;
    private int mMenuItemId = -1;
    private MenuItem mDownloadMenuItem;
    private MenuItem mDoneMenuItem;
    private AlbumsRepository mRepository;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                downloadAlbum();
                return true;

            case R.id.action_done:
                showAlbumDownloaded();
                return true;

            case R.id.action_slideshow:
                startSlideshow(mPhotosAdapter.getPhotos());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void downloadAlbum() {
        setDownloadIndicator(true);
        startAlbumDownload();
        showAlbumDownloadStarted();
    }

    public void startSlideshow(List<Photo> photos) {
        openSlideshow(photos);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mMenuItemId > -1) {
            menu.findItem(mMenuItemId).setVisible(true);
            return true;
        } else {
            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_album_detail, menu);

        mDownloadMenuItem = menu.findItem(R.id.action_download);
        mDoneMenuItem = menu.findItem(R.id.action_done);

        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_album_detail);

        mDownloadProgressBar = findViewById(R.id.download_progressbar);
        mLoading = findViewById(R.id.pb_loading);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Album album = Parcels.unwrap(intent.getParcelableExtra(DownloadService.EXTRA_ALBUM));
                onAlbumDownloaded(album);
            }
        };

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_FINISHED));

        mAlbum = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_ALBUM));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setTitle(mAlbum.getTitle());
        }

        RemoteDataSource remoteDataSource = null;
        boolean showOnlyLocal = getIntent().getBooleanExtra(EXTRA_LOCAL_ONLY, false);
        if (!showOnlyLocal) {
            switch (mAlbum.getRemoteType()) {
                case GOOGLE_PHOTOS:
                    remoteDataSource = PicasaWebDataSource.getInstance();
                    break;

                case UNSPLASH:
                    remoteDataSource = UnsplashDataSource.getInstance();
                    break;
            }
        }

        LocalDataSource localDataSource = LocalDataSource.getInstance(
                getApplicationContext().getContentResolver(),
                new LoaderProvider(this),
                getSupportLoaderManager());

        mRepository = new AlbumsRepository(remoteDataSource, localDataSource);

        mRecyclerView = findViewById(R.id.rv_photos);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        if (!mAlbum.isLocal()) {
            mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                    Timber.d("onLoadMore() page: %d totalItems: %d", page, totalItemsCount);
                    loadAlbumPhotos(page + 1);
                }
            });
        }

        mPhotosAdapter = new PhotosAdapter(this, this);
        mRecyclerView.setAdapter(mPhotosAdapter);

        switch (mAlbum.getDownloadState()) {
            case NOT_DOWNLOADED:
                showDownloadIcon();
                break;

            case DOWNLOADING:
                setDownloadIndicator(true);
                break;

            case DOWNLOADED:
                if (mAlbum.isLocal()) {
                    showSlideshowIcon();
                } else {
                    showDoneIcon();
                }
                break;
        }

        if (savedInstanceState != null) {
            List<Photo> photos = Parcels.unwrap(savedInstanceState.getParcelable(BUNLDE_PHOTOS));
            showAlbumPhotos(photos);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(BUNLDE_RVSTATE));
        } else {
            loadAlbumPhotos(1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(BUNLDE_PHOTOS, Parcels.wrap(mPhotosAdapter.getPhotos()));
        outState.putParcelable(BUNLDE_RVSTATE,
                mRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    public void onDestroy() {
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }

    public void onAlbumDownloaded(Album album) {
        setDownloadIndicator(false);
        showAlbumDownloadFinished();
    }

    public void loadAlbumPhotos(int page) {
        if (page == 1) {
            setLoadingIndicator(true);
        }
        mRepository.getAlbumPhotos(mAlbum, page, this);
    }

    public void setLoadingIndicator(boolean active) {
        if (active) {
            mLoading.setVisibility(View.VISIBLE);
        } else {
            mLoading.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    protected void showDownloadProgressBar() {
        mDownloadProgressBar.setVisibility(View.VISIBLE);
    }

    protected void hideDownloadProgressBar() {
        mDownloadProgressBar.setVisibility(View.INVISIBLE);
    }

    public void setDownloadIndicator(boolean downloading) {
        if (downloading) {
            showDownloadProgressBar();
        } else {
            hideDownloadProgressBar();
        }
    }

    public void showAlbumPhotos(List<Photo> photos) {
        mPhotosAdapter.addPhotos(photos);
    }

    public void showAlbumDownloadStarted() {
        if (mDownloadMenuItem != null) {
            mDownloadMenuItem.setVisible(false);
        }

        Snackbar.make(findViewById(android.R.id.content),
                getResources().getString(R.string.album_download_started),
                Snackbar.LENGTH_LONG).show();
    }

    public void showAlbumDownloadFinished() {
        if (mDoneMenuItem != null) {
            mDoneMenuItem.setVisible(true);
        }

        Snackbar.make(findViewById(android.R.id.content),
                getResources().getString(R.string.album_download_finished),
                Snackbar.LENGTH_LONG).show();
    }

    public void showAlbumDownloaded() {
        Snackbar.make(findViewById(android.R.id.content),
                getResources().getString(R.string.album_downloaded),
                Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onClick(List<Photo> photos, int position) {
        openPhotoGallery(mAlbum, photos, position);
    }

    public void openPhotoGallery(Album album, List<Photo> photos, int position) {
        showPhotoGallery(album, photos, position);
    }

    public void showPhotoGallery(Album album, List<Photo> photos, int position) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(GalleryActivity.EXTRA_PHOTOS, Parcels.wrap(photos));
        intent.putExtra(GalleryActivity.EXTRA_POSITION, position);
        startActivity(intent);
    }

    public void openSlideshow(List<Photo> photos) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_ALBUM, Parcels.wrap(mAlbum));
        intent.putExtra(GalleryActivity.EXTRA_PHOTOS, Parcels.wrap(photos));
        intent.putExtra(GalleryActivity.EXTRA_IS_SLIDESHOW, true);
        startActivity(intent);
    }

    public void startAlbumDownload() {
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.EXTRA_ALBUM, Parcels.wrap(mAlbum));
        startService(intent);
    }

    public void showDoneIcon() {
        mMenuItemId = R.id.action_done;
    }

    public void showDownloadIcon() {
        mMenuItemId = R.id.action_download;
    }

    public void showSlideshowIcon() {
        mMenuItemId = R.id.action_slideshow;
    }

    @Override
    public void onAlbumPhotosLoaded(List<Photo> photos) {
        setLoadingIndicator(false);
        showAlbumPhotos(photos);
    }

    @Override
    public void onDataNotAvailable() {
        setLoadingIndicator(false);
        // TODO: 3/19/2018 show nice error?
    }

    private static class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {
        private Picasso mPicasso;
        private List<Photo> mPhotos;
        private int mDisplayWidth;
        private PhotoClickListener mClickListener;

        PhotosAdapter(Context context, PhotoClickListener clickListener) {
            OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(
                    new OkHttpClient.Builder());
            mPicasso = new Picasso.Builder(context)
                    .downloader(new OkHttp3Downloader(builder.build()))
                    .loggingEnabled(true)
                    .indicatorsEnabled(true) // TODO: 3/19/2018 debug only
                    .build();

            mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

            mClickListener = clickListener;
        }

        void addPhotos(List<Photo> photos) {
            if (mPhotos == null) {
                mPhotos = new ArrayList<>(photos);
            } else {
                mPhotos.addAll(photos);
            }
            notifyDataSetChanged();
        }

        void resetAlbumPhotos() {
            if (mPhotos != null) {
                mPhotos.clear();
                notifyDataSetChanged();
            }
        }

        public List<Photo> getPhotos() {
            return mPhotos;
        }

        @NonNull
        @Override
        public PhotosAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_photo, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PhotosAdapter.ViewHolder holder, int position) {
            final Photo photo = mPhotos.get(position);

            // calculate the resulting height of the image manually and set it to the item view
            // to avoid the flaky behaviour when the images are being loaded and resized by picasso
            holder.itemView.getLayoutParams().height = DisplayHelpers.calculateOptimalPhotoHeight(
                    mDisplayWidth, photo);

            String localPath = photo.getDownloadPath();
            if (localPath != null && localPath.length() > 0) {
                File file = new File(photo.getDownloadPath());
                if (file.exists()) {
                    holder.mPhoto.setImageURI(Uri.fromFile(file));
                }
            } else {
                mPicasso.load(photo.getSmallUri()).into(holder.mPhoto);
            }

            holder.mPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onClick(mPhotos, holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mPhotos == null) {
                return 0;
            } else {
                return mPhotos.size();
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView mPhoto;

            ViewHolder(View itemView) {
                super(itemView);

                mPhoto = itemView.findViewById(R.id.iv_photo);
            }
        }
    }
}
