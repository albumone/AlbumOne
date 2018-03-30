package com.grunskis.albumone.gallery;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;

import java.util.List;

public class GalleryPresenter
        implements GalleryContract.Presenter, Callbacks.GetAlbumPhotosCallback {
    private final Album mAlbum;
    private final AlbumsRepository mRepository;
    private final GalleryContract.View mView;

    GalleryPresenter(Album album, AlbumsRepository repository, GalleryContract.View view) {
        mAlbum = album;
        mRepository = repository;
        mView = view;

        mView.setPresenter(this);
    }

    @Override
    public void loadAlbumPhotos(int page) {
        mRepository.getAlbumPhotos(mAlbum, page, this);
    }

    @Override
    public void start() {

    }

    @Override
    public void onAlbumPhotosLoaded(List<Photo> photos) {
        mView.showAlbumPhotos(photos);
    }

    @Override
    public void onDataNotAvailable() {

    }
}
