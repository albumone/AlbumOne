package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.local.LocalDataSource;

import java.util.List;

public class AlbumsRepository {
    private RemoteDataSource mRemoteDataSource;
    private LocalDataSource mLocalDataSource;

    public AlbumsRepository(RemoteDataSource remoteDataSource,
                            LocalDataSource localDataSource) {
        mRemoteDataSource = remoteDataSource;
        mLocalDataSource = localDataSource;
    }

    public void getAlbumPhotos(final Album album, final int page,
                               final Callbacks.GetAlbumPhotosCallback callback) {
        if (mRemoteDataSource != null) {
            getAlbumPhotosFromRemoteDataSource(album, page, callback);
        } else {
            mLocalDataSource.getAlbumPhotos(album, callback);
        }
    }

    private void getAlbumPhotosFromRemoteDataSource(
            Album album, int page, final Callbacks.GetAlbumPhotosCallback callback) {
        mRemoteDataSource.getAlbumPhotos(album, page, new Callbacks.GetAlbumPhotosCallback() {
            @Override
            public void onAlbumPhotosLoaded(List<Photo> photos) {
                callback.onAlbumPhotosLoaded(photos);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }

    public boolean supportsPaging() {
        return mRemoteDataSource.supportsPaging();
    }
}
