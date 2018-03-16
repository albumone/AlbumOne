package com.grunskis.albumone.data.source.local;

import android.content.ContentValues;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

public class AlbumValues {
    public static ContentValues from(Album album) {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_TITLE, album.getTitle());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_PATH,
                album.getCoverPhoto().getDownloadPath());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_WIDTH,
                album.getCoverPhoto().getWidth());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_HEIGHT,
                album.getCoverPhoto().getHeight());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_REMOTE_ID,
                album.getCoverPhoto().getRemoteId());
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_ID,
                album.getRemoteId());
        int remoteType = album.getRemoteType().getValue();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_TYPE,
                remoteType);
        return values;
    }

    public static ContentValues from(Photo photo) {
        ContentValues values = new ContentValues();
        Album album = photo.getAlbum();
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID, album.getId());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_URL, photo.getDownloadPath());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_WIDTH, photo.getWidth());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_HEIGHT, photo.getHeight());
        values.put(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID,
                photo.getRemoteId());
        return values;
    }

    public static ContentValues albumCoverPhoto(Photo photo) {
        ContentValues values = new ContentValues();
        values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_PATH,
                photo.getDownloadPath());
        return values;
    }
}
