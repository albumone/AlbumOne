package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface AlbumOneRemoteDataSource {
    interface GetAlbumsCallback {
        void onAlbumsLoaded(List<Album> albums);
        void onDataNotAvailable();
    }
    void getAlbums(GetAlbumsCallback callback);

    interface GetAlbumPhotosCallback {
        void onAlbumPhotosLoaded(List<Photo> photos);
        void onDataNotAvailable();
    }
    void getAlbumPhotos(Album album, GetAlbumPhotosCallback callback);
}
