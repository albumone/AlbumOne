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
import com.grunskis.albumone.data.Photo;

import org.parceler.Parcels;

import java.util.List;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.gallery.EXTRA_ALBUM";
    public static final String EXTRA_POSITION = "com.grunskis.albumone.gallery.EXTRA_POSITION";
    public static final String EXTRA_PHOTOS = "com.grunskis.albumone.gallery.EXTRA_PHOTOS";
    public static final String EXTRA_IS_SLIDESHOw = "com.grunskis.albumone.gallery.EXTRA_IS_SLIDESHOw";

    private static final int SLIDESHOW_DELAY_MILLIS = 5000; // 5 seconds

    private Handler mSlideshowHandler;
    private Runnable mSlideshowRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        toggleLowProfileMode();

        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        boolean isSlideshow = intent.getBooleanExtra(EXTRA_IS_SLIDESHOw, false);
        int position = intent.getIntExtra(EXTRA_POSITION, 0);
        List<Photo> photos = Parcels.unwrap(intent.getParcelableExtra(EXTRA_PHOTOS));

        GalleryAdapter adapter = new GalleryAdapter(this, this);
        adapter.setPhotos(photos);

        final ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);

        if (isSlideshow) {
            mSlideshowRunnable = new Runnable() {
                @Override
                public void run() {
                    // TODO: 3/27/2018 add a nice transition
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, false);
                    mSlideshowHandler.postDelayed(this, SLIDESHOW_DELAY_MILLIS);
                }
            };

            mSlideshowHandler = new Handler();
            mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY_MILLIS);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            // TODO: 3/27/2018 use theme with version qualifier instead
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
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
}
