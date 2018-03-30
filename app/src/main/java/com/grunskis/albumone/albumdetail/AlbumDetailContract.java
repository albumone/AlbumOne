package com.grunskis.albumone.albumdetail;

import com.grunskis.albumone.BasePresenter;
import com.grunskis.albumone.BaseView;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface AlbumDetailContract {
    interface View extends BaseView<AlbumDetailContract.Presenter> {
        void setLoadingIndicator(boolean active);

        void setDownloadIndicator(boolean downloading);

        void showAlbumPhotos(List<Photo> photos);

        void showAlbumDownloadStarted();

        void showAlbumDownloadFinished();

        void showAlbumDownloaded();

        void showPhotoGallery(Album album, List<Photo> photos, int position);

        void openSlideshow(List<Photo> photos);

        void startAlbumDownload();

        void showDoneIcon();

        void showDownloadIcon();

        void showSlideshowIcon();

        void resetAlbumPhotos();
    }

    interface Presenter extends BasePresenter {
        void loadAlbumPhotos(int page);

        void downloadAlbum();

        void onAlbumDownloaded(Album album);

        void openPhotoGallery(Album album, List<Photo> photos, int position);

        void startSlideshow(List<Photo> photos);

        void showAlbumDownloaded();
    }
}
