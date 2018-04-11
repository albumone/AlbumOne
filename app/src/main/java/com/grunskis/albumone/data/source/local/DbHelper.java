package com.grunskis.albumone.data.source.local;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.Photo;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    public static Album getAlbumById(Context context, long albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                AlbumOnePersistenceContract.AlbumEntry.buildUriWith(albumId),
                AlbumOnePersistenceContract.AlbumEntry.COLUMNS,
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{String.valueOf(albumId)},
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

    public static long createPhoto(ContentResolver contentResolver, Photo photo) {
        Uri uri = contentResolver.insert(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumValues.from(photo));

        return ContentUris.parseId(uri);
    }

    public static void updateAlbumCoverPhoto(ContentResolver contentResolver, Album album,
                                             Photo photo) {
        contentResolver.update(
                AlbumOnePersistenceContract.AlbumEntry.buildUriWith(album.getId()),
                AlbumValues.albumCoverPhoto(photo),
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{String.valueOf(album.getId())}
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

    public static Photo getPhotoById(Context context, long id) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                AlbumOnePersistenceContract.PhotoEntry.buildUriWith(id),
                AlbumOnePersistenceContract.PhotoEntry.COLUMNS,
                AlbumOnePersistenceContract.PhotoEntry._ID + " = ?",
                new String[]{String.valueOf(id)},
                null);

        Photo photo = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                photo = Photo.from(cursor);
            }
            cursor.close();
        }
        return photo;
    }

    public static Photo getPhotoByRemoteId(Context context, String remoteId) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumOnePersistenceContract.PhotoEntry.COLUMNS,
                AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID + " = ?",
                new String[]{remoteId},
                null);

        Photo photo = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                photo = Photo.from(cursor);
            }
            cursor.close();
        }
        return photo;
    }

    public static void updateAlbumAfterRefresh(ContentResolver contentResolver, Album album,
                                               ContentValues values) {
        contentResolver.update(
                AlbumOnePersistenceContract.AlbumEntry.buildUriWith(album.getId()),
                values,
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{String.valueOf(album.getId())}
        );
    }

    public static void deletePhotoByRemoteId(ContentResolver contentResolver, String remoteId) {
        contentResolver.delete(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID + " = ?",
                new String[]{remoteId});
    }

    public static List<Photo> getAlbumPhotosByAlbumId(Context context, long albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumOnePersistenceContract.PhotoEntry.COLUMNS,
                AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID + " = ?",
                new String[]{String.valueOf(albumId)},
                null);

        List<Photo> photos = new ArrayList<>();
        if (cursor != null) {
            photos.addAll(LocalDataSource.Photos.from(cursor));
            cursor.close();
        }
        return photos;
    }

    public static Download getDownload(Context context, String albumRemoteId) {
        ContentResolver contentResolver = context.getContentResolver();
        String where = AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID + " = ?";
        String[] selectionArgs = new String[]{albumRemoteId};
        Cursor cursor = contentResolver.query(
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

}
