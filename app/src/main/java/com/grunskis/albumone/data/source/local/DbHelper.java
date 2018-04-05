package com.grunskis.albumone.data.source.local;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.grunskis.albumone.data.Album;

public class DbHelper {
    public static Album getAlbumById(Context context, String albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                AlbumOnePersistenceContract.AlbumEntry.buildUriWith(albumId),
                AlbumOnePersistenceContract.AlbumEntry.ALBUM_COLUMNS,
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{albumId},
                null);

        Album album = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                album = Album.from(cursor);
            }
            cursor.close();
        }
        return album;
    }
}
