package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.local.LocalDataSource;

import java.util.List;

public class AlbumsRepository {
//    private static AlbumsRepository INSTANCE = null;

    private RemoteDataSource mRemoteDataSource;
    private LocalDataSource mLocalDataSource;

    public AlbumsRepository(RemoteDataSource remoteDataSource,
                            LocalDataSource localDataSource) {
        mRemoteDataSource = remoteDataSource;
        mLocalDataSource = localDataSource;
    }

//    public static AlbumsRepository getInstance(RemoteDataSource remoteDataSource,
//                                               LocalDataSource localDataSource) {
//        if (INSTANCE == null) {
//            INSTANCE = new AlbumsRepository(remoteDataSource, localDataSource);
//        }
//        return INSTANCE;
//    }

    public void getAlbums(final int page, final Callbacks.GetAlbumsCallback callback) {
        if (mRemoteDataSource != null) {
            getAlbumsFromRemoteDataSource(page, callback);
        } else {
            mLocalDataSource.getAlbums(callback);
        }
    }

    private void getAlbumsFromRemoteDataSource(int page, final Callbacks.GetAlbumsCallback callback) {
        mRemoteDataSource.getAlbums(page, new Callbacks.GetAlbumsCallback() {
            @Override
            public void onAlbumsLoaded(List<Album> albums) {
                callback.onAlbumsLoaded(albums);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }

    public void saveAlbum(Album album, List<Photo> photos) {
        mLocalDataSource.saveAlbum(album, photos);
    }

    public void deleteAllAlbums() {
        mLocalDataSource.deleteAllAlbums();
    }

    public void deleteAlbumPhotos(String albumId) {
        mLocalDataSource.deleteAlbumPhotos(albumId);
    }

    public void savePhoto(Photo photo) {
        mLocalDataSource.savePhoto(photo);
    }

    public void getAlbumPhotos(final Album album, final int page,
                               final Callbacks.GetAlbumPhotosCallback callback) {
        if (mRemoteDataSource != null) {
            getAlbumPhotosFromRemoteDataSource(album, page, callback);
        } else {
            mLocalDataSource.getAlbumPhotos(album, callback);
        }
    }

    private void getAlbumPhotosFromRemoteDataSource(Album album, int page, final Callbacks.GetAlbumPhotosCallback callback) {
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

    public Download getDownload(String albumRemoteId) {
        return mLocalDataSource.getDownload(albumRemoteId);
    }
}
