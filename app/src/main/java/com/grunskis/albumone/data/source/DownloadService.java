package com.grunskis.albumone.data.source;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.local.DbHelper;
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
    public static final String BROADCAST_DOWNLOAD_FINISHED = "BROADCAST_DOWNLOAD_FINISHED";

    private ContentResolver mContentResolver;
    private RemoteDataSource mRepository;
    private OkHttpClient mClient;
    private PhotoDownloadCallback photoDownloadCallback = new PhotoDownloadCallback() {
        @Override
        public void onPhotoDownloaded(Photo photo, String path) {
            photo.setDownloadPath(path);

            DbHelper.createPhoto(mContentResolver, photo);
        }

        @Override
        public void onPhotoDownloadError(Photo photo) {
            Timber.e("Failed to download photo! id: %s url: %s", photo.getRemoteId(), photo.getSmallUri());
        }
    };
    private long mDownloadId;

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

        switch (album.getRemoteType()) {
            case GOOGLE_PHOTOS:
                mRepository = PicasaWebDataSource.getInstance();
                break;

            case UNSPLASH:
                mRepository = UnsplashDataSource.getInstance();
                break;

            default:
                return;
        }

        // save album to the DB so that it gets an ID assigned
        // we don't set the cover image here yet, it will be set later after it's downloaded
        long albumId = DbHelper.createAlbum(mContentResolver, album);
        album.setId(albumId);

        mDownloadId = DbHelper.createDownloadEntry(mContentResolver, album);

        downloadAlbumPhotos(album);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void downloadAlbumPhotos(final Album album) {
        final File albumDir = getPrivateAlbumStorageDir(this, album.getRemoteId());

        // download album cover photo
        Photo coverPhoto = album.getCoverPhoto();
        coverPhoto.setAlbum(album);
        downloadPhoto(albumDir, coverPhoto, new PhotoDownloadCallback() {
            @Override
            public void onPhotoDownloaded(Photo photo, String path) {
                photoDownloadCallback.onPhotoDownloaded(photo, path);

                // update album with the cover photo data
                DbHelper.updateAlbumCoverPhoto(mContentResolver, album, photo);
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

    private File getPrivateAlbumStorageDir(Context context, String albumTitle) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumTitle);
        if (!file.mkdirs()) {
            Timber.e("Directory not created?");
        }
        return file;
    }

    private void downloadAlbumPhotos(final Album album, final int page, final DownloadPhotosListener callback) {
        mRepository.getAlbumPhotos(album, page, new Callbacks.GetAlbumPhotosCallback() {
            @Override
            public void onAlbumPhotosLoaded(List<Photo> photos) {
                Timber.i("Downloading album photos: title: %s page %d",
                        album.getTitle(), page);
                callback.downloadPhotos(photos);

                downloadAlbumPhotos(album, page + 1, callback);
            }

            @Override
            public void onDataNotAvailable() {
                Timber.i("Downloading album photos finished! title: %s", album.getTitle());

                DbHelper.updateDownloadEntry(mContentResolver, mDownloadId);

                album.setDownloadState(Album.DownloadState.DOWNLOADED);
                broadcastDownloadFinished(album);
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

    private void broadcastDownloadFinished(Album album) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_FINISHED);
        intent.putExtra(EXTRA_ALBUM, Parcels.wrap(album));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    interface DownloadPhotosListener {
        void downloadPhotos(List<Photo> photos);
    }

    interface PhotoDownloadCallback {
        void onPhotoDownloaded(Photo photo, String path);

        void onPhotoDownloadError(Photo photo);
    }
}
