package com.grunskis.albumone.albums;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.local.LocalDataSource;

public class AlbumsActivity extends AppCompatActivity {
    private FloatingActionButton mFABAdd;
    private FloatingActionButton mFABUnsplash;
    private FloatingActionButton mFABGooglePhotos;
    private boolean mIsFABOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_albums);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                startActivity(intent);
                closeFABMenu();
            }
        });

        mFABGooglePhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AlbumsActivity.this, PicasawebAlbumsActivity.class);
                startActivity(intent);
                closeFABMenu();
            }
        });

        // TODO: 3/16/2018 get rid of duplication
        LoaderProvider loaderProvider = new LoaderProvider(this);

        AlbumsFragment albumsFragment =
                (AlbumsFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (albumsFragment == null) {
            albumsFragment = AlbumsFragment.newInstance(true);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.content_frame, albumsFragment);
            transaction.commit();
        }

//        AlbumsRepository repository = Injection.provideAlbumsRepository(
//                getApplicationContext(),
//                loaderProvider,
//                getSupportLoaderManager());

        AlbumsRepository repository = new AlbumsRepository(
                null,
                LocalDataSource.getInstance(
                        getApplicationContext().getContentResolver(),
                        loaderProvider,
                        getSupportLoaderManager()));

        new AlbumsPresenter(repository, albumsFragment);
    }

    // TODO: 3/16/2018 should this be in the presenter?
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
}
