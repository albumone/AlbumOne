package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;

public interface RemoteDataSource {
    boolean isAuthenticated();
    void setAuthToken(String authToken);

    boolean supportsPaging();

    void getAlbum(String remoteId, Callbacks.GetAlbumCallback callback);
    void getAlbums(int page, Callbacks.GetAlbumsCallback callback);
    void getAlbumPhotos(Album album, int page, Callbacks.GetAlbumPhotosCallback callback);
}
