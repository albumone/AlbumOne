package com.grunskis.albumone.data.source;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.grunskis.albumone.AlbumOneApplication;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.local.AlbumOnePersistenceContract;
import com.grunskis.albumone.data.source.local.DbHelper;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;
import com.grunskis.albumone.data.source.remote.UnsplashDataSource;

import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    public static final String EXTRA_IS_REFRESH =
            "com.grunskis.albumone.data.source.EXTRA_IS_REFRESH";

    public static final String BROADCAST_DOWNLOAD_STARTED = "BROADCAST_DOWNLOAD_STARTED";
    public static final String BROADCAST_DOWNLOAD_FINISHED = "BROADCAST_DOWNLOAD_FINISHED";
    public static final String BROADCAST_DOWNLOAD_UPTODATE = "BROADCAST_DOWNLOAD_UPTODATE";

    private ContentResolver mContentResolver;
    private RemoteDataSource mRepository;
    private OkHttpClient mClient;
    private PhotoDownloadCallback photoDownloadCallback = new PhotoDownloadCallback() {
        @Override
        public void onPhotoDownloaded(Photo photo, String path) {
            photo.setDownloadPath(path);

            long id = DbHelper.createPhoto(mContentResolver, photo);
            photo.setId(id);
        }

        @Override
        public void onPhotoDownloadError(Photo photo) {
            Timber.e("Failed to download photo! id: %s url: %s", photo.getRemoteId(),
                    photo.getSmallUri());
        }
    };
    private long mDownloadId;
    private Tracker mAnalyticsTracker;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContentResolver = getApplicationContext().getContentResolver();
        mClient = new OkHttpClient();
        mAnalyticsTracker = ((AlbumOneApplication) getApplication()).getDefaultTracker();
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
        boolean isRefresh = intent.getBooleanExtra(EXTRA_IS_REFRESH, false);

        switch (album.getRemoteType()) {
            case GOOGLE_PHOTOS:
                mRepository = PicasaWebDataSource.getInstance(this);
                break;

            case UNSPLASH:
                mRepository = UnsplashDataSource.getInstance(this);
                break;

            default:
                return;
        }

        if (!isRefresh) {
            // save album to the DB so that it gets an ID assigned
            // we don't set the cover image here yet, it will be set later after it's downloaded
            long albumId = DbHelper.createAlbum(mContentResolver, album);
            album.setId(albumId);
        }

        mDownloadId = DbHelper.createDownloadEntry(mContentResolver, album);

        File albumDir = getPrivateAlbumStorageDir(this, album.getRemoteId());

        if (isRefresh) {
            refreshAlbumPhotos(albumDir, album);
        } else {
            downloadAlbumPhotos(albumDir, album);
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void downloadAlbumPhotos(final File albumDir, final Album album) {
        mAnalyticsTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Download album " + album.getRemoteType())
                .build());

        // download album cover photo
        Photo coverPhoto = album.getCoverPhoto();
        coverPhoto.setAlbum(album);

        broadcastDownloadStarted(album);

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

            @Override
            public void downloadFinished(Album album) {
            }
        });
    }

    private File getPrivateAlbumStorageDir(Context context, String albumTitle) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumTitle);
        file.mkdirs();
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
                callback.downloadFinished(album);

                DbHelper.updateDownloadEntry(mContentResolver, mDownloadId);

                album.setDownloadState(Album.DownloadState.DOWNLOADED);
                broadcastDownloadFinished(album);
            }
        });
    }

    private void downloadPhoto(File directory, final Photo photo,
                               final PhotoDownloadCallback callback) {
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
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    if (responseBody == null) {
                        throw new IOException("Response body is null");
                    }

                    BufferedSink sink = Okio.buffer(Okio.sink(photoFile));
                    sink.writeAll(responseBody.source());
                    sink.close();
                    responseBody.close();

                    callback.onPhotoDownloaded(photo, photoFile.getPath());
                }
            }
        });
    }

    private void refreshAlbumPhotos(final File albumDir, final Album album) {
        // create a list of photo remote IDs we currently have in the DB
        final List<String> photoIds = new ArrayList<>();
        for (Photo photo : DbHelper.getAlbumPhotosByAlbumId(this, album.getId())) {
            photoIds.add(photo.getRemoteId());
        }

        mRepository.getAlbum(album.getRemoteId(), new Callbacks.GetAlbumCallback() {
            @Override
            public void onAlbumLoaded(final Album otherAlbum) {
                if (otherAlbum.getUpdatedAt().after(album.getUpdatedAt())) {
                    Timber.i("Refreshing album %s...", album.getTitle());

                    downloadAlbumPhotos(album, 1, new DownloadPhotosListener() {
                        @Override
                        public void downloadPhotos(List<Photo> photos) {
                            for (Photo photo : photos) {
                                String remoteId = photo.getRemoteId();
                                if (photoIds.contains(remoteId)) {
                                    // photo still exists in the remote album
                                    photoIds.remove(remoteId);
                                }

                                Photo existingPhoto = DbHelper.getPhotoByRemoteId(
                                        mContentResolver, remoteId);
                                if (existingPhoto == null) {
                                    Timber.i("Downloading new photo remoteId: %s uri: %s",
                                            remoteId, photo.getSmallUri());
                                    downloadPhoto(albumDir, photo, photoDownloadCallback);
                                }
                            }
                        }

                        @Override
                        public void downloadFinished(Album album) {
                            for (String remoteId : photoIds) {
                                Timber.i("Deleting photo remoteId: %s", remoteId);
                                Photo photo = DbHelper.getPhotoByRemoteId(
                                        mContentResolver, remoteId);
                                File file = new File(photo.getDownloadPath());
                                if (file.delete()) {
                                    Timber.i("Deleting photo file %s", file.getAbsoluteFile());
                                }
                                DbHelper.deletePhotoByRemoteId(mContentResolver, remoteId);
                            }

                            ContentValues values = new ContentValues();
                            Photo coverPhoto = otherAlbum.getCoverPhoto();
                            if (!coverPhoto.getRemoteId().equals(album.getCoverPhoto().getRemoteId())) {
                                Photo p = DbHelper.getPhotoByRemoteId(
                                        mContentResolver, coverPhoto.getRemoteId());
                                if (p != null) {
                                    Timber.i("Setting a new cover photo for album title: %s",
                                            otherAlbum.getTitle());
                                    values.put(
                                            AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID,
                                            p.getId());

                                    // update cover photo id for the album we're refreshing
                                    // later the model values will get updated from the DB
                                    album.getCoverPhoto().setId(p.getId());
                                }
                            }
                            values.put(AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_UPDATED_AT,
                                    otherAlbum.getUpdatedAt().getTime());
                            DbHelper.updateAlbumAfterRefresh(mContentResolver, album, values);
                        }
                    });
                } else {
                    Timber.i("Album %s up to date %s", album.getTitle(),
                            album.getUpdatedAt().toString());

                    broadcastDownloadUpToDate(album);
                }
            }

            @Override
            public void onDataNotAvailable() {
                Timber.e("Failed to get album %s", album.getTitle());
            }
        });
    }

    private void broadcastDownloadStarted(Album album) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_STARTED);
        intent.putExtra(EXTRA_ALBUM, Parcels.wrap(album));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastDownloadFinished(Album album) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_FINISHED);
        intent.putExtra(EXTRA_ALBUM, Parcels.wrap(album));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastDownloadUpToDate(Album album) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_UPTODATE);
        intent.putExtra(EXTRA_ALBUM, Parcels.wrap(album));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    interface DownloadPhotosListener {
        void downloadPhotos(List<Photo> photos);

        void downloadFinished(Album album);
    }

    interface PhotoDownloadCallback {
        void onPhotoDownloaded(Photo photo, String path);

        void onPhotoDownloadError(Photo photo);
    }
}
