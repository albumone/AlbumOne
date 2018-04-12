package com.grunskis.albumone.data.source.local;

import android.content.ContentValues;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

public class AlbumValues {
    public static ContentValues from(Album album) {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_TITLE, album.getTitle());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID,
                album.getCoverPhoto().getId());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_ID,
                album.getRemoteId());
        int remoteType = album.getRemoteType().getValue();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_TYPE,
                remoteType);
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_UPDATED_AT,
                album.getUpdatedAt().getTime());
        return values;
    }

    public static ContentValues from(Photo photo) {
        ContentValues values = new ContentValues();
        Album album = photo.getAlbum();
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID, album.getId());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_URL,
                photo.getUri().toString());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_WIDTH, photo.getWidth());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_HEIGHT, photo.getHeight());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID,
                photo.getRemoteId());
        return values;
    }

    public static ContentValues albumCoverPhoto(Photo photo) {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID,
                photo.getId());
        return values;
    }

    public static ContentValues downloadStartedEntry(Album album) {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID,
                album.getRemoteId());
        values.put(AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_STARTED_AT,
                System.currentTimeMillis());
        return values;
    }

    public static ContentValues downloadFinishedEntry() {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_FINISHED_AT,
                System.currentTimeMillis());
        return values;
    }
}
