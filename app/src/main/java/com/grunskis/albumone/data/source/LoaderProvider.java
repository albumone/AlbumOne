package com.grunskis.albumone.data.source;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;

public class LoaderProvider {
    private final Context mContext;

    public LoaderProvider(Context context) {
        mContext = context;
    }

    public Loader<Cursor> createAlbumsLoader() {
        return new CursorLoader(
                mContext,
                AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI,
                AlbumOnePersistenceContract.AlbumEntry.ALBUM_COLUMNS,
                null,
                null,
                null
        );
    }

    public Loader<Cursor> createAlbumPhotosLoader(String albumId) {
        return new CursorLoader(
                mContext,
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumOnePersistenceContract.PhotoEntry.PHOTO_COLUMNS,
                AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID + " = ?",
                new String[]{albumId},
                null
        );
    }
}
