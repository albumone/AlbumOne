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

    AlbumDetailPresenter(Album album, AlbumsRepository repository,
                         AlbumDetailContract.View view) {
        mAlbum = album;
        mRepository = repository;
        mView = view;

        mView.setPresenter(this);
    }

    @Override
    public void start() {
        // TODO: 3/29/2018 reseting on back is not the best. should restore from cache instead.
        resetAlbumPhotos();

        loadAlbumPhotos(1);

        switch (mAlbum.getDownloadState()) {
            case NOT_DOWNLOADED:
                mView.showDownloadIcon();
                break;

            case DOWNLOADING:
                mView.setDownloadIndicator(true);
                break;

            case DOWNLOADED:
                if (mAlbum.isLocal()) {
                    mView.showSlideshowIcon();
                } else {
                    mView.showDoneIcon();
                }
                break;
        }
    }

    public void loadAlbumPhotos(int page) {
        if (page == 1) {
            mView.setLoadingIndicator(true);
        }
        mRepository.getAlbumPhotos(mAlbum, page, this);
    }

    @Override
    public void onAlbumPhotosLoaded(List<Photo> photos) {
        mView.setLoadingIndicator(false);
        mView.showAlbumPhotos(photos);
    }

    @Override
    public void onDataNotAvailable() {
        mView.setLoadingIndicator(false);
        // TODO: 3/19/2018 show nice error?
    }

    @Override
    public void downloadAlbum() {
        mView.setDownloadIndicator(true);
        mView.startAlbumDownload();
        mView.showAlbumDownloadStarted();
    }

    @Override
    public void onAlbumDownloaded(Album album) {
        mView.setDownloadIndicator(false);
        mView.showAlbumDownloadFinished();
    }

//    @Override
//    public void onAlbumDownloadFailed() {
//        mView.setDownloadIndicator(false);
//        mView.showAlbumDownloadFailed();
//    }

    @Override
    public void openPhotoGallery(Album album, List<Photo> photos, int position) {
        mView.showPhotoGallery(album, photos, position);
    }

    @Override
    public void startSlideshow(List<Photo> photos) {
        mView.openSlideshow(photos);
    }

    @Override
    public void showAlbumDownloaded() {
        mView.showAlbumDownloaded();
    }

    private void resetAlbumPhotos() {
        mView.resetAlbumPhotos();
    }
}
