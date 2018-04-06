package com.grunskis.albumone.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;
import com.grunskis.albumone.data.source.local.DbHelper;

import org.parceler.Parcel;

@Parcel
public class Photo {
    Long mId;
    Album mAlbum;
    Uri mSmallUri;
    int mWidth;
    int mHeight;
    String mDownloadPath;
    String mRemoteId;

    public Photo() {
    }

    public Photo(Long id, Album album, String smallUri, int width, int height, String remoteId) {
        mId = id;
        mAlbum = album;
        // TODO: 4/6/2018 use uris also for local files
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

    public Photo(Album album, String smallUri, int width, int height, String remoteId) {
        this(null, album, smallUri, width, height, remoteId);
    }

    public static Photo from(Cursor cursor) {
        Long id = cursor.getLong(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry._ID));
        String url = cursor.getString(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_URL));
        int width = cursor.getInt(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_WIDTH));
        int height = cursor.getInt(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_HEIGHT));
        String remoteId = cursor.getString(
                cursor.getColumnIndex(AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID));
        return new Photo(id, null, url, width, height, remoteId);
    }

    public Long getId() {
        return mId;
    }

    public void setId(Long id) {
        mId = id;
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

    public void refreshFromDb(Context context) {
        Photo photo = DbHelper.getPhotoById(context, mId);

        mSmallUri = photo.getSmallUri();
        mDownloadPath = photo.getDownloadPath();
        mWidth = photo.getWidth();
        mHeight = photo.getHeight();
        mRemoteId = photo.getRemoteId();
    }
}
