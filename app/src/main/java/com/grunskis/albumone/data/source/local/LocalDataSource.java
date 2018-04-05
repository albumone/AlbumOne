package com.grunskis.albumone.data.source.local;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.LoaderProvider;

import java.util.ArrayList;
import java.util.List;

public class LocalDataSource {
    private static final int ALBUMS_LOADER = 1;
    private static final int PHOTOS_LOADER = 2;

    private static LocalDataSource INSTANCE;

    private ContentResolver mContentResolver;
    private LoaderProvider mLoaderProvider;
    private LoaderManager mLoaderManager;

    private LocalDataSource(ContentResolver contentResolver, LoaderProvider loaderProvider,
                            LoaderManager loaderManager) {
        mContentResolver = contentResolver;
        mLoaderProvider = loaderProvider;
        mLoaderManager = loaderManager;
    }

    public static LocalDataSource getInstance(ContentResolver contentResolver,
                                              LoaderProvider loaderProvider,
                                              LoaderManager loaderManager) {
        //if (INSTANCE == null) {
        // can't be a singleton b/c it preserves the context of activity, which is gone after rotation
            INSTANCE = new LocalDataSource(contentResolver, loaderProvider, loaderManager);
        //}
        return INSTANCE;
    }

    public void getAlbums(final Callbacks.GetAlbumsCallback callback) {
        LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks =
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @NonNull
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                        return mLoaderProvider.createAlbumsLoader();
                    }

                    @Override
                    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                        if (data != null) {
                            callback.onAlbumsLoaded(Albums.from(data));
                        } else {
                            callback.onDataNotAvailable();
                        }
                    }

                    @Override
                    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                    }
                };

        if (mLoaderManager.getLoader(ALBUMS_LOADER) == null) {
            mLoaderManager.initLoader(ALBUMS_LOADER, null, loaderCallbacks);
        } else {
            mLoaderManager.restartLoader(ALBUMS_LOADER, null, loaderCallbacks);
        }
    }

    private long saveAlbum(Album album) {
        Uri uri = mContentResolver.insert(
                AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI,
                AlbumValues.from(album));

        return ContentUris.parseId(uri);
    }

    public void saveAlbum(Album album, List<Photo> photos) {
        album.setId(saveAlbum(album));

        for (Photo photo : photos) {
            photo.setAlbum(album);
            savePhoto(photo);
        }
    }

    public void deleteAllAlbums() {
        mContentResolver.delete(AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI, null, null);
        mContentResolver.delete(AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI, null, null);
    }

    public void savePhoto(Photo photo) {
        ContentValues values = AlbumValues.from(photo);
        mContentResolver.insert(AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI, values);
    }

    public void deleteAlbumPhotos(String albumId) {
        String where = AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID + " = ?";
        String[] selectionArgs = new String[]{albumId};
        mContentResolver.delete(AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                where, selectionArgs);
    }

    public Download getDownload(String albumRemoteId) {
        String where = AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID + " = ?";
        String[] selectionArgs = new String[]{albumRemoteId};
        Cursor cursor = mContentResolver.query(
                AlbumOnePersistenceContract.DownloadEntry.CONTENT_URI,
                AlbumOnePersistenceContract.DownloadEntry.COLUMNS,
                where, selectionArgs, null);

        Download download = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                download = Download.from(cursor);
            }
            cursor.close();
        }
        return download;
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

    private static class Albums {
        public static List<Album> from(Cursor cursor) {
            ArrayList<Album> albums = new ArrayList<>();
            while (cursor.moveToNext()) {
                albums.add(Album.from(cursor));
            }
            return albums;
        }
    }

    private static class Photos {
        public static List<Photo> from(Cursor cursor) {
            ArrayList<Photo> photos = new ArrayList<>();
            while (cursor.moveToNext()) {
                photos.add(Photo.from(cursor));
            }
            return photos;
        }
    }
}
