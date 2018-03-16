package com.grunskis.albumone.data.source;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;
import com.grunskis.albumone.data.source.local.AlbumValues;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

public class DownloadService extends IntentService {
    public static final String EXTRA_ALBUM = "com.grunskis.albumone.data.source.EXTRA_ALBUM";

    private ContentResolver mContentResolver;
    private AlbumsRepository mRepository;
    private OkHttpClient mClient;
    private PhotoDownloadCallback photoDownloadCallback = new PhotoDownloadCallback() {
        @Override
        public void onPhotoDownloaded(Photo photo, String path) {
            photo.setDownloadPath(path);

            savePhoto(photo);
        }

        @Override
        public void onPhotoDownloadError(Photo photo) {
            Timber.e("Failed to download photo! id: %s url: %s", photo.getRemoteId(), photo.getSmallUri());
        }
    };

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContentResolver = getApplicationContext().getContentResolver();
        mClient = new OkHttpClient();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isExternalStorageWritable()) {
            Timber.e("External storage not writable!");
            return;
        }

        if (intent == null) {
            Timber.e("Intent passed to activity is null!");
            return;
        }
        Album album = Parcels.unwrap(intent.getParcelableExtra(EXTRA_ALBUM));

        RemoteDataSource remoteDataSource = null;
        switch (album.getRemoteType()) {
            case GOOGLE_PHOTOS:
                remoteDataSource = PicasaWebDataSource.getInstance();
                break;

            case UNSPLASH:
                remoteDataSource = UnsplashDataSource.getInstance();
                break;
        }
        mRepository = new AlbumsRepository(remoteDataSource, null);

        downloadAlbumPhotos(album);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getPrivateAlbumStorageDir(Context context, String albumTitle) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumTitle);
        if (!file.mkdirs()) {
            Timber.e("Directory not created?");
        }
        return file;
    }

    private void downloadAlbumPhotos(final Album album) {
        final File albumDir = getPrivateAlbumStorageDir(this, album.getRemoteId());

        // save album to the DB so that it gets an ID assigned
        // we don't set the cover image here yet, it will be set later after it's downloaded
        album.setId(saveAlbum(album));

        // download album cover photo
        Photo coverPhoto = album.getCoverPhoto();
        coverPhoto.setAlbum(album);
        downloadPhoto(albumDir, coverPhoto, new PhotoDownloadCallback() {
            @Override
            public void onPhotoDownloaded(Photo photo, String path) {
                photoDownloadCallback.onPhotoDownloaded(photo, path);

                // update album with the cover photo data
                updateAlbumCoverPhoto(album, photo);
            }

            @Override
            public void onPhotoDownloadError(Photo photo) {
                photoDownloadCallback.onPhotoDownloadError(photo);
            }
        });

        // download remaining album photos
        downloadAlbumPhotos(album, 1, new DownloadPhotosListener() {
            @Override
            public void downloadPhotos(List<Photo> photos) {
                for (final Photo photo : photos) {
                    if (album.getCoverPhoto().getRemoteId().equals(photo.getRemoteId())) {
                        // this is album cover photo which we have already downloaded
                        continue;
                    }

                    downloadPhoto(albumDir, photo, photoDownloadCallback);
                }
            }
        });
    }

    private void downloadAlbumPhotos(final Album album, final int page, final DownloadPhotosListener callback) {
        mRepository.getAlbumPhotos(album, page, new Callbacks.GetAlbumPhotosCallback() {
            @Override
            public void onAlbumPhotosLoaded(List<Photo> photos) {
                callback.downloadPhotos(photos);

                downloadAlbumPhotos(album, page + 1, callback);
            }

            @Override
            public void onDataNotAvailable() {
            }
        });
    }

    private void downloadPhoto(File directory, final Photo photo, final PhotoDownloadCallback callback) {
        final File photoFile = new File(directory, photo.getFilename());

        Request request = new Request.Builder().url(photo.getSmallUri().toString()).build();

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Timber.e(e);

                callback.onPhotoDownloadError(photo);
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);
                    if (responseBody == null) throw new IOException("Response body is null");

                    BufferedSink sink = Okio.buffer(Okio.sink(photoFile));
                    sink.writeAll(responseBody.source());
                    sink.close();
                    responseBody.close();

                    callback.onPhotoDownloaded(photo, photoFile.getPath());
                }
            }
        });
    }

    // TODO: 3/22/2018 this is just copy pasted from the repository.
    // maybe make repository not dependend on loader manger?
    private long saveAlbum(Album album) {
        Uri uri = mContentResolver.insert(
                AlbumOnePersistenceContract.AlbumEntry.CONTENT_URI,
                AlbumValues.from(album));

        return ContentUris.parseId(uri);
    }

    private void savePhoto(Photo photo) {
        mContentResolver.insert(
                AlbumOnePersistenceContract.PhotoEntry.CONTENT_URI,
                AlbumValues.from(photo));
    }

    private void updateAlbumCoverPhoto(Album album, Photo photo) {
        mContentResolver.update(
                AlbumOnePersistenceContract.AlbumEntry.buildAlbumsUriWith(album.getId()),
                AlbumValues.albumCoverPhoto(photo),
                AlbumOnePersistenceContract.AlbumEntry._ID + " = ?",
                new String[]{album.getId()}
        );
    }

    interface DownloadPhotosListener {
        void downloadPhotos(List<Photo> photos);
    }

    interface PhotoDownloadCallback {
        void onPhotoDownloaded(Photo photo, String path);

        void onPhotoDownloadError(Photo photo);
    }
}
