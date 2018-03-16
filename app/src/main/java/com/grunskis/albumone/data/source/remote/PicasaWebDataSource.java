package com.grunskis.albumone.data.source.remote;

import android.net.Uri;
import android.os.AsyncTask;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.RemoteDataSource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PicasaWebDataSource implements RemoteDataSource {
    private static final String API_PREFIX
            = "https://picasaweb.google.com/data/feed/api/user/";

    private static PicasaWebDataSource INSTANCE = null;

    private PicasawebService mPicasaClient;

    private PicasaWebDataSource() {
        mPicasaClient = new PicasawebService("albumone");
    }

    public static PicasaWebDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PicasaWebDataSource();
        }
        return INSTANCE;
    }

    public void setAuthToken(String authToken) {
        mPicasaClient.setUserToken(authToken);
    }

    @Override
    public void getAlbums(int page, final Callbacks.GetAlbumsCallback callback) {
        new AlbumsLoader(mPicasaClient, callback, page).execute();
    }

    @Override
    public void getAlbumPhotos(final Album album, int page, final Callbacks.GetAlbumPhotosCallback callback) {
        new AlbumPhotosLoader(mPicasaClient, callback, page).execute(album);
    }

    private static class AlbumsLoader extends AsyncTask<Void, Void, List<Album>> {
        private Callbacks.GetAlbumsCallback mAlbumsCallback;
        private PicasawebService mPicassaService;
        private int mStartIndex;

        AlbumsLoader(PicasawebService service, Callbacks.GetAlbumsCallback callback, int page) {
            mAlbumsCallback = callback;
            mPicassaService = service;
            mStartIndex = (page - 1) * 10 + 1;
        }

        @Override
        protected List<Album> doInBackground(Void... voids) {
            URL albumUrl = null;
            try {
                // TODO: 3/21/2018 figure out how to use Query
                // to set query parameters instead of hard-coding them in the url
                albumUrl = new URL(API_PREFIX + "default?thumbsize=1280&start-index=" + String.valueOf(mStartIndex) + "&max-results=10");
            } catch (MalformedURLException e) {
                Timber.e(e);
            }
            if (albumUrl == null) {
                return null;
            }

            UserFeed userFeed = null;
            try {
                userFeed = mPicassaService.getFeed(albumUrl, UserFeed.class);
            } catch (IOException | ServiceException e) {
                Timber.e(e);
            }
            if (userFeed == null) {
                return null;
            }

            List<Album> albums = new ArrayList<>();
            for (GphotoEntry entry : userFeed.getEntries()) {
                AlbumEntry ae = new AlbumEntry(entry);
                String remoteId = Uri.parse(ae.getId()).getLastPathSegment();
                String title = ae.getTitle().getPlainText();
                Photo coverPhoto = null;

                try {
                    // not using the getMediaThumbnails here b/c it returns wrong height for
                    // the album thumbnail
                    // HACK: showing album only in one of the first 10 entries is an image (not video)
                    URL feedUrl = new URL(ae.getFeedLink().getHref() + "?start-index=1&max-results=10&imgmax=1280");
                    AlbumFeed feed = mPicassaService.getFeed(feedUrl, AlbumFeed.class);
                    List<GphotoEntry> entries = feed.getEntries();
                    if (entries != null && entries.size() > 0) {
                        for (GphotoEntry photoEntry : entries) {
                            PhotoEntry pe = new PhotoEntry(photoEntry);

                            List<MediaContent> mediaContents = pe.getMediaContents();
                            for (MediaContent mediaContent : mediaContents) {
                                // videos are not supported at the moment
                                // the only easy way to achieve this I found this
                                // comparing medium is not reliable due to video thumbnails
                                if (mediaContent.getType().equals("image/jpeg")) {
                                    String photoUrl = mediaContent.getUrl();
                                    int width = mediaContent.getWidth();
                                    int height = mediaContent.getHeight();
                                    String photoRemoteId = Uri.parse(pe.getId()).getLastPathSegment();
                                    coverPhoto = new Photo(null, photoUrl, width, height, photoRemoteId);
                                    break;
                                }
                            }
                            if (coverPhoto != null) {
                                break;
                            }
                        }
                    }
                } catch (ServiceException | IOException e) {
                    Timber.e(e);
                    continue;
                }

                if (coverPhoto != null) {
                    albums.add(new Album(title, coverPhoto, remoteId, RemoteType.GOOGLE_PHOTOS));
                }
            }
            return albums;
        }

        @Override
        protected void onPostExecute(List<Album> albums) {
            if (albums != null && albums.size() > 0) {
                mAlbumsCallback.onAlbumsLoaded(albums);
            } else {
                mAlbumsCallback.onDataNotAvailable();
            }
        }
    }

    private static class AlbumPhotosLoader extends AsyncTask<Album, Void, List<Photo>> {
        private final PicasawebService mPicasaService;
        private final Callbacks.GetAlbumPhotosCallback mGetAlbumPhotosCallback;
        private final int mStartIndex;

        AlbumPhotosLoader(PicasawebService service, Callbacks.GetAlbumPhotosCallback callback, int page) {
            mPicasaService = service;
            mGetAlbumPhotosCallback = callback;
            mStartIndex = (page - 1) * 10 + 1;
        }

        @Override
        protected List<Photo> doInBackground(Album... albums) {
            URL albumUrl = null;
            try {
                albumUrl = new URL(API_PREFIX + "default/albumid/" + albums[0].getRemoteId() + "?max-results=10&imgmax=1280&start-index=" + String.valueOf(mStartIndex));
            } catch (MalformedURLException e) {
                Timber.e(e);
            }
            if (albumUrl == null) {
                return null;
            }

            AlbumFeed albumFeed = null;
            try {
                albumFeed = mPicasaService.getFeed(albumUrl, AlbumFeed.class);
            } catch (IOException | ServiceException e) {
                Timber.e(e);
            }
            if (albumFeed == null) {
                return null;
            }

            List<Photo> photos = new ArrayList<>();
            for (GphotoEntry entry : albumFeed.getEntries()) {
                PhotoEntry pe = new PhotoEntry(entry);

                List<MediaContent> mediaContents = pe.getMediaContents();
                for (MediaContent mediaContent : mediaContents) {
                    // videos are not supported at the moment
                    // the only easy way to achieve this I found this
                    // comparing medium is not reliable due to video thumbnails
                    if (mediaContent.getType().equals("image/jpeg")) {
                        int width = mediaContent.getWidth();
                        int height = mediaContent.getHeight();
                        String remoteId = Uri.parse(pe.getId()).getLastPathSegment();
                        String url = mediaContent.getUrl();
                        photos.add(new Photo(albums[0], url, width, height, remoteId));
                    }
                }

            }
            return photos;
        }

        @Override
        protected void onPostExecute(List<Photo> albums) {
            if (albums != null && albums.size() > 0) {
                mGetAlbumPhotosCallback.onAlbumPhotosLoaded(albums);
            } else {
                mGetAlbumPhotosCallback.onDataNotAvailable();
            }
        }
    }
}
