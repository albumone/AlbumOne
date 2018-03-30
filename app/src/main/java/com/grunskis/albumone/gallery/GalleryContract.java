package com.grunskis.albumone.gallery;

import com.grunskis.albumone.BasePresenter;
import com.grunskis.albumone.BaseView;
import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface GalleryContract {
    interface Presenter extends BasePresenter {
        void loadAlbumPhotos(int page);
    }

    interface View extends BaseView<Presenter> {
        void showAlbumPhotos(List<Photo> photos);
    }
}
