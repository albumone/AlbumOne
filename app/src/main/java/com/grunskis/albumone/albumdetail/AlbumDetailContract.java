package com.grunskis.albumone.albumdetail;

import com.grunskis.albumone.BasePresenter;
import com.grunskis.albumone.BaseView;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface AlbumDetailContract {
    interface View extends BaseView<AlbumDetailContract.Presenter> {
        void setLoadingIndicator(boolean active);

        void showAlbumPhotos(List<Photo> photos);

        //void showAlbumSaved();
        void showAlbumAlbumDownloadStarted();

        void showPhotoGallery(Album album, List<Photo> photos, int position);
    }
    interface Presenter extends BasePresenter {
        void loadAlbumPhotos(int page);

        //void downloadAlbum();
        void openPhotoGallery(Album album, List<Photo> photos, int position);
    }
}
