package com.grunskis.albumone.data;

import android.database.Cursor;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;

import org.parceler.Parcel;

import java.util.Date;

@Parcel
public class Album {
    Long mId;
    String mTitle;
    Photo mCoverPhoto;
    String mRemoteId;
    RemoteType mRemoteType;
    DownloadState mDownloadState;
    Date mUpdatedAt;

    public DownloadState getDownloadState() {
        return mDownloadState;
    }

    public Album() {}

    public Album(String title, Photo coverPhoto, String remoteId, RemoteType remoteType,
                 long updatedAt) {
        this(null, title, coverPhoto, remoteId, remoteType, updatedAt);
    }

    public Album(Long id, String title, Photo coverPhoto, String remoteId,
                 RemoteType remoteType, long updatedAt) {
        mId = id;
        mTitle = title;
        mCoverPhoto = coverPhoto;
        mRemoteId = remoteId;
        mRemoteType = remoteType;
        if (updatedAt > 0) {
            mUpdatedAt = new Date(updatedAt);
        }
    }

    public static Album from(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry._ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(
                        AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_TITLE));
        long coverPhotoId = cursor.getLong(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID));
        String remoteId = cursor.getString(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_ID));
        RemoteType remoteType = RemoteType.forValue(cursor.getInt(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_TYPE)));
        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_UPDATED_AT));

        Photo coverPhoto = new Photo();
        coverPhoto.setId(coverPhotoId);

        return new Album(id, title, coverPhoto, remoteId, remoteType, updatedAt);
    }

    public Long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public Photo getCoverPhoto() {
        return mCoverPhoto;
    }

    public String getRemoteId() {
        return mRemoteId;
    }

    public RemoteType getRemoteType() {
        return mRemoteType;
    }

    public boolean isLocal() {
        return mCoverPhoto.getUri().toString().startsWith("file:");
    }

    public void setDownloadState(DownloadState downloadState) {
        mDownloadState = downloadState;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public enum DownloadState {
        NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
    }
}
