package com.grunskis.albumone.gallery;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Photo;

import org.parceler.Parcels;

import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.gallery.EXTRA_ALBUM";
    public static final String EXTRA_POSITION = "com.grunskis.albumone.gallery.EXTRA_POSITION";
    public static final String EXTRA_PHOTOS = "com.grunskis.albumone.gallery.EXTRA_PHOTOS";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);

        int position = getIntent().getIntExtra(EXTRA_POSITION, 0);
        List<Photo> photos = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_PHOTOS));

        GalleryAdapter adapter = new GalleryAdapter(this);
        adapter.setPhotos(photos);

        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
    }
}
