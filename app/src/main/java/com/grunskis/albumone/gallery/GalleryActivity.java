package com.grunskis.albumone.gallery;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.LocalDataSource;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

import org.parceler.Parcels;

import java.util.List;

public class GalleryActivity
        extends AppCompatActivity
        implements View.OnClickListener, Callbacks.GetAlbumPhotosCallback {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.gallery.EXTRA_ALBUM";
    public static final String EXTRA_POSITION = "com.grunskis.albumone.gallery.EXTRA_POSITION";
    public static final String EXTRA_IS_SLIDESHOW = "com.grunskis.albumone.gallery.EXTRA_IS_SLIDESHOw";
    public static final String EXTRA_PHOTOS = "com.grunskis.albumone.gallery.EXTRA_PHOTOS";

    private static final String BUNDLE_VPSTATE = "BUNDLE_VPSTATE";
    private static final String BUNDLE_PHOTOS = "BUNDLE_PHOTOS";

    private static final int SLIDESHOW_DELAY_MILLIS = 5000;

    private Album mAlbum;
    private GalleryAdapter mAdapter;
    private AlbumsRepository mRepository;
    private ViewPager mViewPager;
    private Handler mSlideshowHandler;
    private Runnable mSlideshowRunnable;
    private boolean mIsSlideshow;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(BUNDLE_VPSTATE, mViewPager.onSaveInstanceState());
        outState.putParcelable(BUNDLE_PHOTOS, Parcels.wrap(mAdapter.getPhotos()));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        toggleLowProfileMode();

        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        mIsSlideshow = intent.getBooleanExtra(EXTRA_IS_SLIDESHOW, false);
        mAlbum = Parcels.unwrap(intent.getParcelableExtra(EXTRA_ALBUM));

        mAdapter = new GalleryAdapter(this, this);

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mAdapter);

        mViewPager.addOnPageChangeListener(new ViewPagerLoadMoreListener(mAdapter) {
            @Override
            public void onLoadMore(int page) {
                loadAlbumPhotos(page);
            }
        });

        // TODO: 3/27/2018 use theme with version qualifier instead
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(getColor(android.R.color.black));
        }

        RemoteDataSource remoteDataSource = null;
        if (!mAlbum.isLocal()) {
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

        if (savedInstanceState != null) {
            List<Photo> photos = Parcels.unwrap(savedInstanceState.getParcelable(BUNDLE_PHOTOS));
            mAdapter.addPhotos(photos);
            mViewPager.onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_VPSTATE));
            startSlideshow(photos);
        } else {
            List<Photo> photos = null;
            if (intent.hasExtra(EXTRA_PHOTOS)) {
                photos = Parcels.unwrap(intent.getParcelableExtra(EXTRA_PHOTOS));
            }
            if (photos != null && photos.size() > 0) {
                mAdapter.addPhotos(photos);

                int position = intent.getIntExtra(EXTRA_POSITION, 0);
                mViewPager.setCurrentItem(position);

                startSlideshow(photos);
            } else {
                loadAlbumPhotos(1);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mSlideshowHandler != null) {
            mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
        }

        super.onDestroy();
    }

    // Low profile mode covers the nav & status bar icons with black
    // so they're less distracting.
    private void toggleLowProfileMode() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();

        uiOptions ^= View.SYSTEM_UI_FLAG_LOW_PROFILE;

        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onClick(View view) {
        toggleLowProfileMode();
    }

    public void showAlbumPhotos(List<Photo> photos) {
        mAdapter.addPhotos(photos);
    }

    public void loadAlbumPhotos(int page) {
        mRepository.getAlbumPhotos(mAlbum, page, this);
    }

    @Override
    public void onAlbumPhotosLoaded(List<Photo> photos) {
        showAlbumPhotos(photos);
        startSlideshow(photos);
    }

    @Override
    public void onDataNotAvailable() {
        // TODO: 4/4/2018 implement
    }

    private void startSlideshow(final List<Photo> photos) {
        if (mIsSlideshow && mSlideshowRunnable == null) {
            mSlideshowRunnable = new Runnable() {
                @Override
                public void run() {
                    // TODO: 3/27/2018 add a nice transition
                    int nextItem = mViewPager.getCurrentItem() + 1;
                    if (nextItem >= photos.size()) {
                        nextItem = 0;
                    }
                    mViewPager.setCurrentItem(nextItem, false);
                    mSlideshowHandler.postDelayed(this, SLIDESHOW_DELAY_MILLIS);
                }
            };

            mSlideshowHandler = new Handler();
            mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY_MILLIS);
        }
    }
}
