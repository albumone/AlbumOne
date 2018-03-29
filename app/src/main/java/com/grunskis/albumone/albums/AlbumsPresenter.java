package com.grunskis.albumone.albums;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Download;
import com.grunskis.albumone.data.source.AlbumsRepository;
import com.grunskis.albumone.data.source.Callbacks;

import java.util.List;

public class AlbumsPresenter implements AlbumsContract.Presenter,
        Callbacks.GetAlbumsCallback {

    private final AlbumsRepository mAlbumsRepository;
    private final AlbumsContract.View mAlbumsView;

    AlbumsPresenter(AlbumsRepository albumsRepository, AlbumsContract.View albumsView) {
        mAlbumsRepository = albumsRepository;
        mAlbumsView = albumsView;

        mAlbumsView.setPresenter(this);
    }

    @Override
    public void start() {
        resetAlbums();
        loadAlbums(1);
    }

    private void resetAlbums() {
        mAlbumsView.resetAlbums();
    }

    @Override
    public void loadAlbums(int page) {
        mAlbumsView.setLoadingIndicator(true);
        mAlbumsRepository.getAlbums(page, this);
    }

    @Override
    public void openAlbumDetails(Album album) {
        mAlbumsView.showAlbumDetails(album);
    }

    @Override
    public void onAlbumsLoaded(List<Album> albums) {
        mAlbumsView.setLoadingIndicator(false);

        for (Album album : albums) {
            Download download = mAlbumsRepository.getDownload(album.getRemoteId());
            if (download == null) {
                album.setDownloadState(Album.DownloadState.NOT_DOWNLOADED);
            } else {
                if (download.getFinishedAt() == null) {
                    album.setDownloadState(Album.DownloadState.DOWNLOADING);
                } else {
                    album.setDownloadState(Album.DownloadState.DOWNLOADED);
                }
            }
        }

        mAlbumsView.showAlbums(albums);
    }

    @Override
    public void onDataNotAvailable() {
        mAlbumsView.setLoadingIndicator(false);
        // TODO: 1/29/2018 show some error
    }

    @Override
    public void onAlbumDownloaded(Album album) {
        mAlbumsView.updateAlbum(album);
    }
}
