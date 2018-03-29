package com.grunskis.albumone.data;

import android.database.Cursor;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;

import java.util.Date;

public class Download {
    private String mAlbumRemoteId;
    private Date mStartedAt;
    private Date mFinishedAt;
    private Date mFailedAt;

    private Download(String albumRemoteId, long startedAt, long finishedAt, long failedAt) {
        mAlbumRemoteId = albumRemoteId;
        if (startedAt > 0) {
            mStartedAt = new Date(startedAt);
        }
        if (finishedAt > 0) {
            mFinishedAt = new Date(finishedAt);
        }
        if (failedAt > 0) {
            mFailedAt = new Date(failedAt);
        }
    }

    public static Download from(Cursor cursor) {
        int indexAlbumRemoteId = cursor.getColumnIndex(
                AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID);
        int indexStartedAt = cursor.getColumnIndex(
                AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_STARTED_AT);
        int indexFinishedAt = cursor.getColumnIndex(
                AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_FINISHED_AT);
        int indexFailedAt = cursor.getColumnIndex(
                AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_FAILED_AT);

        return new Download(
                cursor.getString(indexAlbumRemoteId),
                cursor.getLong(indexStartedAt),
                cursor.getLong(indexFinishedAt),
                cursor.getLong(indexFailedAt));
    }

    public Date getFinishedAt() {
        return mFinishedAt;
    }
}
