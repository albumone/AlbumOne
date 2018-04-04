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
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

import org.parceler.Parcels;

import java.util.List;

public class GalleryActivity
        extends AppCompatActivity
        implements View.OnClickListener, Callbacks.GetAlbumPhotosCallback {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.gallery.EXTRA_ALBUM";
    public static final String EXTRA_POSITION = "com.grunskis.albumone.gallery.EXTRA_POSITION";
    public static final String EXTRA_PHOTOS = "com.grunskis.albumone.gallery.EXTRA_PHOTOS";
    public static final String EXTRA_IS_SLIDESHOW = "com.grunskis.albumone.gallery.EXTRA_IS_SLIDESHOw";

    // TODO: 3/29/2018 read this value from settings
    private static final int SLIDESHOW_DELAY_MILLIS = 5000;

    private Album mAlbum;
    private GalleryAdapter mAdapter;
    private AlbumsRepository mRepository;

    private Handler mSlideshowHandler;
    private Runnable mSlideshowRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        toggleLowProfileMode();

        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        boolean isSlideshow = intent.getBooleanExtra(EXTRA_IS_SLIDESHOW, false);
        int position = intent.getIntExtra(EXTRA_POSITION, 0);
        final List<Photo> photos = Parcels.unwrap(intent.getParcelableExtra(EXTRA_PHOTOS));
        mAlbum = Parcels.unwrap(intent.getParcelableExtra(EXTRA_ALBUM));

        mAdapter = new GalleryAdapter(this, this);
        mAdapter.setPhotos(photos);

        final ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(mAdapter);
        viewPager.setCurrentItem(position);

        viewPager.addOnPageChangeListener(new ViewPagerLoadMoreListener(mAdapter) {
            @Override
            public void onLoadMore(int page) {
                loadAlbumPhotos(page);
            }
        });

        if (Build.VERSION.SDK_INT >= 21) {
            // TODO: 3/27/2018 use theme with version qualifier instead
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
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

        mRepository = new AlbumsRepository(remoteDataSource, null);

        if (isSlideshow) {
            mSlideshowRunnable = new Runnable() {
                @Override
                public void run() {
                    // TODO: 3/27/2018 add a nice transition
                    int nextItem = viewPager.getCurrentItem() + 1;
                    if (nextItem >= photos.size()) {
                        nextItem = 0;
                    }
                    viewPager.setCurrentItem(nextItem, false);
                    mSlideshowHandler.postDelayed(this, SLIDESHOW_DELAY_MILLIS);
                }
            };

            mSlideshowHandler = new Handler();
            mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY_MILLIS);
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
        toggleLowProfileMode(); // TODO: 4/4/2018 figure out why this doesn't work anymore
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
    }

    @Override
    public void onDataNotAvailable() {
        // TODO: 4/4/2018 implement
    }
}
