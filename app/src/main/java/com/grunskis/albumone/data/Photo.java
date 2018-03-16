package com.grunskis.albumone.data;

import android.database.Cursor;
import android.net.Uri;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;

import org.parceler.Parcel;

@Parcel
public class Photo {
    Album mAlbum;
    Uri mSmallUri;
    int mWidth;
    int mHeight;
    String mDownloadPath;
    String mRemoteId;

    public Photo() {
    }

    public Photo(Album album, String smallUri, int width, int height, String remoteId) {
        mAlbum = album;
        if (smallUri != null) {
            if (smallUri.startsWith("http")) {
                mSmallUri = Uri.parse(smallUri);
            } else {
                mDownloadPath = smallUri;
            }
        }
        mWidth = width;
        mHeight = height;
        mRemoteId = remoteId;
    }

    public static Photo from(Cursor cursor) {
        String url = cursor.getString(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_URL));
        int width = cursor.getInt(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_WIDTH));
        int height = cursor.getInt(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_HEIGHT));
        String remoteId = cursor.getString(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID));
        return new Photo(null, url, width, height, remoteId);
    }

    public Uri getSmallUri() {
        return mSmallUri;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album album) {
        mAlbum = album;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getFilename() {
        String format = mSmallUri.getQueryParameter("fm");
        if (format == null) {
            format = "jpg";
        }
        return mRemoteId + "." + format;
    }

    public String getDownloadPath() {
        return mDownloadPath;
    }

    public void setDownloadPath(String path) {
        mDownloadPath = path;
    }

    public String getRemoteId() {
        return mRemoteId;
    }
}
