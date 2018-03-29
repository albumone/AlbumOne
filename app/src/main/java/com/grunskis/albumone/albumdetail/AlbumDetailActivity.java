package com.grunskis.albumone.albumdetail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.LoaderProvider;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.data.source.local.LocalDataSource;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

import org.parceler.Parcels;

import timber.log.Timber;

// TODO: 3/27/2018 restore recycler view state on rotation
public class AlbumDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.albumdetail.EXTRA_ALBUM";
    public static final String EXTRA_LOCAL_ONLY = "com.grunskis.albumone.albumdetail.EXTRA_LOCAL_ONLY";

    private Album mAlbum;
    private boolean mShowLocalOnly;
    private ProgressBar mDownloadProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_album_detail);

        mDownloadProgressBar = findViewById(R.id.download_progressbar);

        if (savedInstanceState != null) {
            Timber.w("savedInstanceState");
        }

        mAlbum = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_ALBUM));
        mShowLocalOnly = getIntent().getBooleanExtra(EXTRA_LOCAL_ONLY, false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setTitle(mAlbum.getTitle());
        }

        AlbumDetailFragment albumDetailFragment =
                (AlbumDetailFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);
        if (albumDetailFragment == null) {
            albumDetailFragment = AlbumDetailFragment.newInstance(mAlbum);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.content_frame, albumDetailFragment);
            transaction.commit();
        }


        RemoteDataSource remoteDataSource = null;
        if (!mShowLocalOnly) {
            switch (mAlbum.getRemoteType()) {
                case GOOGLE_PHOTOS:
                    remoteDataSource = PicasaWebDataSource.getInstance();
                    break;

                case UNSPLASH:
                    remoteDataSource = UnsplashDataSource.getInstance();
                    break;
            }
        }

        LoaderProvider loaderProvider = new LoaderProvider(this);

        LocalDataSource localDataSource = LocalDataSource.getInstance(
                getApplicationContext().getContentResolver(),
                loaderProvider,
                getSupportLoaderManager());

//        AlbumsRepository repository = Injection.provideAlbumsRepository(
//                getApplicationContext(),
//                loaderProvider,
//                getSupportLoaderManager());

        AlbumsRepository repository = new AlbumsRepository(remoteDataSource, localDataSource);

        new AlbumDetailPresenter(mAlbum, repository, albumDetailFragment);
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
}
