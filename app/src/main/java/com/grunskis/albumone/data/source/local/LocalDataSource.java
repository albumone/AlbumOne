package com.grunskis.albumone.data.source.local;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.LoaderProvider;

import java.util.ArrayList;
import java.util.List;

public class LocalDataSource {
    private static final int PHOTOS_LOADER = 2;

    private LoaderProvider mLoaderProvider;
    private LoaderManager mLoaderManager;

    private LocalDataSource(LoaderProvider loaderProvider, LoaderManager loaderManager) {
        mLoaderProvider = loaderProvider;
        mLoaderManager = loaderManager;
    }

    public static LocalDataSource getInstance(LoaderProvider loaderProvider,
                                              LoaderManager loaderManager) {
        return new LocalDataSource(loaderProvider, loaderManager);
    }

    public void getAlbumPhotos(final Album album, final Callbacks.GetAlbumPhotosCallback callback) {
        LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks =
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @NonNull
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                        return mLoaderProvider.createAlbumPhotosLoader(album.getId());
                    }

                    @Override
                    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                        if (data != null) {
                            callback.onAlbumPhotosLoaded(Photos.from(data));
                        } else {
                            callback.onDataNotAvailable();
                        }
                    }

                    @Override
                    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                    }
                };

        if (mLoaderManager.getLoader(PHOTOS_LOADER) == null) {
            mLoaderManager.initLoader(PHOTOS_LOADER, null, loaderCallbacks);
        } else {
            mLoaderManager.restartLoader(PHOTOS_LOADER, null, loaderCallbacks);
        }
    }

    public static class Albums {
        public static List<Album> from(Cursor cursor) {
            // caller is responsible for closing the cursor
            List<Album> albums = new ArrayList<>();

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                albums.add(Album.from(cursor));
            }

            return albums;
        }
    }

    public static class Photos {
        public static List<Photo> from(Cursor cursor) {
            // caller is responsible for closing the cursor
            List<Photo> photos = new ArrayList<>();

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                photos.add(Photo.from(cursor));
            }

            return photos;
        }
    }
}
