package com.grunskis.albumone.data.source.local;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

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

    public static long createDownloadEntry(ContentResolver contentResolver, Album album) {
        Uri uri = contentResolver.insert(
                AlbumOnePersistenceContract.DownloadEntry.CONTENT_URI,
                AlbumValues.downloadStartedEntry(album));

        return ContentUris.parseId(uri);
    }

    public static long createAlbum(ContentResolver contentResolver, Album album) {
        Uri uri = contentResolver.insert(
                AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI,
                AlbumValues.from(album));

        return ContentUris.parseId(uri);
    }

    public static void createPhoto(ContentResolver contentResolver, Photo photo) {
        contentResolver.insert(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumValues.from(photo));
    }

    public static void updateAlbumCoverPhoto(ContentResolver contentResolver, Album album,
                                             Photo photo) {
        contentResolver.update(
                AlbumOnePersistenceContract.AlbumEntry.buildUriWith(album.getId()),
                AlbumValues.albumCoverPhoto(photo),
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{album.getId()}
        );
    }

    public static void updateDownloadEntry(ContentResolver contentResolver, long downloadId) {
        contentResolver.update(
                AlbumOnePersistenceContract.DownloadEntry.buildUriWith(downloadId),
                AlbumValues.downloadFinishedEntry(),
                AlbumOnePersistenceContract.DownloadEntry._ID + " = ?",
                new String[]{String.valueOf(downloadId)}
        );
    }
}
