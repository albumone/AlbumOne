package com.grunskis.albumone.albumdetail;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;

import java.util.List;

public class AlbumDetailPresenter implements AlbumDetailContract.Presenter,
        Callbacks.GetAlbumPhotosCallback {

    private final Album mAlbum;
    private final AlbumsRepository mRepository;
    private final AlbumDetailContract.View mView;

    //private List<Photo> mPhotos;

    AlbumDetailPresenter(Album album, AlbumsRepository repository,
                         AlbumDetailContract.View view) {
        mAlbum = album;
        mRepository = repository;
        mView = view;

        mView.setPresenter(this);
    }

    @Override
    public void start() {
        loadAlbumPhotos(1);
    }

    public void loadAlbumPhotos(int page) {
        mView.setLoadingIndicator(true);
        mRepository.getAlbumPhotos(mAlbum, page, this);
    }

    @Override
    public void onAlbumPhotosLoaded(List<Photo> photos) {
        mView.setLoadingIndicator(false);
        //mPhotos = new ArrayList<>(photos);
        mView.showAlbumPhotos(photos);
    }

    @Override
    public void onDataNotAvailable() {
        mView.setLoadingIndicator(false);
        // TODO: 3/19/2018 show nice error?
    }

//    public void downloadAlbum() {
//        // TODO: 3/13/2018 implement saving in a service or something
//        mRepository.downloadAlbumPhotos(mAlbum);
//        mView.showAlbumSaved();
//    }

    @Override
    public void openPhotoGallery(Album album, List<Photo> photos, int position) {
        mView.showPhotoGallery(album, photos, position);
    }
}
