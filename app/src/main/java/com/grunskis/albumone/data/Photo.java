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
    Uri mUri;
    int mWidth;
    int mHeight;
    String mRemoteId;

    public Photo() {
    }

    public Photo(Long id, Album album, String uri, int width, int height, String remoteId) {
        mId = id;
        mAlbum = album;
        mUri = Uri.parse(uri);
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

    public Uri getUri() {
        return mUri;
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

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public String getRemoteId() {
        return mRemoteId;
    }

    public String getFilename() {
        String format = mUri.getQueryParameter("fm");
        if (format == null) {
            format = "jpg";
        }
        return mRemoteId + "." + format;
    }

    public void refreshFromDb(Context context) {
        Photo photo = DbHelper.getPhotoById(context, mId);

        mUri = photo.getUri();
        mWidth = photo.getWidth();
        mHeight = photo.getHeight();
        mRemoteId = photo.getRemoteId();
    }
}
