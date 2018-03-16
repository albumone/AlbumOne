package com.grunskis.albumone.albums;

import com.grunskis.albumone.BasePresenter;
import com.grunskis.albumone.BaseView;
import com.grunskis.albumone.data.Album;

import java.util.List;

public interface AlbumsContract {
    interface View extends BaseView<Presenter> {
        void setLoadingIndicator(boolean active);

        void showAlbums(List<Album> albums);

        void showAlbumDetails(Album album);

        void resetAlbums();
    }

    interface Presenter extends BasePresenter {
        void loadAlbums(int page);
        void openAlbumDetails(Album album);
    }
}
