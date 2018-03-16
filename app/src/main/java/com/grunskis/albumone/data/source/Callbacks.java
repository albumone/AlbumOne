package com.grunskis.albumone.data.source;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface Callbacks {
    interface GetAlbumsCallback {
        void onAlbumsLoaded(List<Album> albums);

        void onDataNotAvailable();
    }

    interface GetAlbumPhotosCallback {
        void onAlbumPhotosLoaded(List<Photo> photos);

        void onDataNotAvailable();
    }
}
