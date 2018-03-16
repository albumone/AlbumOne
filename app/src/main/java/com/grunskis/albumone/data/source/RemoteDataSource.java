package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;

public interface RemoteDataSource {
    void setAuthToken(String authToken);

    void getAlbums(int page, Callbacks.GetAlbumsCallback callback);

    void getAlbumPhotos(Album album, int page, Callbacks.GetAlbumPhotosCallback callback);
}
