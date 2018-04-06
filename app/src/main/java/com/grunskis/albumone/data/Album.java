package com.grunskis.albumone.data;

import android.database.Cursor;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;

import org.parceler.Parcel;

@Parcel
public class Album {
    // TODO: 3/21/2018 read more about parceler to avoid these warnings
    String mId;
    String mTitle;
    Photo mCoverPhoto;
    String mRemoteId;
    RemoteType mRemoteType;
    DownloadState mDownloadState;

    public DownloadState getDownloadState() {
        return mDownloadState;
    }

    public Album() {}

    public Album(String title, Photo coverPhoto, String remoteId, RemoteType remoteType) {
        this(null, title, coverPhoto, remoteId, remoteType);
    }

    public Album(String id, String title, Photo coverPhoto, String remoteId, RemoteType remoteType) {
        mId = id;
        mTitle = title;
        mCoverPhoto = coverPhoto;
        mRemoteId = remoteId;
        mRemoteType = remoteType;
    }

    public static Album from(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry._ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(
                        AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_TITLE));
        long coverPhotoId = cursor.getLong(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID));
        String remoteId = cursor.getString(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_ID));
        RemoteType remoteType = RemoteType.forValue(cursor.getInt(cursor.getColumnIndexOrThrow(
                AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_TYPE)));

        Photo coverPhoto = new Photo();
        coverPhoto.setId(coverPhotoId);

        return new Album(id, title, coverPhoto, remoteId, remoteType);
    }

    public String getId() {
        return mId;
    }

    public void setId(long id) {
        mId = String.valueOf(id);
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
        return mCoverPhoto.getDownloadPath() != null;
    }

    public void setDownloadState(DownloadState downloadState) {
        mDownloadState = downloadState;
    }

    public enum DownloadState {
        NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
    }
}
