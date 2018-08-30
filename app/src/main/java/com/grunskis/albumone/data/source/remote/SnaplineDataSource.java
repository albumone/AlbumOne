package com.grunskis.albumone.data.source.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.util.StethoUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import timber.log.Timber;

public class SnaplineDataSource implements RemoteDataSource {

    private static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN_SNAPLINE";
    private static final String API_BASE_URL = "https://api.snapline.io/v1/";

    private static SnaplineDataSource INSTANCE = null;

    private SharedPreferences mSharedPreferences;

    private SnaplineDataSource(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SnaplineDataSource getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SnaplineDataSource(context);
        }
        return INSTANCE;
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthToken() != null;
    }

    private String getAuthToken() {
        return mSharedPreferences.getString(PREF_AUTH_TOKEN, null);
    }

    @Override
    public void setAuthToken(String authToken) {
        mSharedPreferences.edit().putString(PREF_AUTH_TOKEN, authToken).apply();
    }

    @Override
    public boolean supportsPaging() {
        return false;
    }

    @Override
    public void getAlbum(String remoteId, final Callbacks.GetAlbumCallback callback) {
        SnaplineAPI api = getApiClient();

        api.getAlbum(remoteId).enqueue(new Callback<SnaplineAlbum>() {
            @Override
            public void onResponse(@NonNull Call<SnaplineAlbum> call,
                                   @NonNull Response<SnaplineAlbum> response) {
                if (response.isSuccessful()) {
                    SnaplineAlbum snaplineAlbum = response.body();
                    if (snaplineAlbum != null) {
                        Photo coverPhoto = new Photo(null,
                                snaplineAlbum.coverPhoto.url, 800,
                                600, snaplineAlbum.coverPhoto.id);
                        long updatedAt = parseTimestamp(snaplineAlbum.updatedAt);

                        callback.onAlbumLoaded(new Album(snaplineAlbum.title, coverPhoto,
                                snaplineAlbum.id, RemoteType.SNAPLINE, updatedAt));
                    } else {
                        callback.onDataNotAvailable();
                    }
                } else {
                    try {
                        ResponseBody error = response.errorBody();
                        if (error != null) {
                            Timber.e("Failed to load collections! error: %s",
                                    error.string());
                        } else {
                            Timber.e("Failed to load collections! error: null");
                        }
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    callback.onDataNotAvailable();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SnaplineAlbum> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    @Override
    public void getAlbums(int page, final Callbacks.GetAlbumsCallback callback) {
        SnaplineAPI api = getApiClient();

        api.getAlbums().enqueue(new Callback<List<SnaplineAlbum>>() {
            @Override
            public void onResponse(@NonNull Call<List<SnaplineAlbum>> call,
                                   @NonNull Response<List<SnaplineAlbum>> response) {
                List<SnaplineAlbum> snaplineAlbums = response.body();
                if (response.isSuccessful()) {
                    if (snaplineAlbums != null && snaplineAlbums.size() > 0) {
                        List<Album> albums = new ArrayList<>();

                        for (SnaplineAlbum snaplineAlbum : snaplineAlbums) {
                            Timber.i("title: %s", snaplineAlbum.title);

                            Photo coverPhoto = new Photo(null,
                                    snaplineAlbum.coverPhoto.url, 800,
                                    600, snaplineAlbum.coverPhoto.id);
                            long updatedAt = parseTimestamp(snaplineAlbum.updatedAt);

                            albums.add(new Album(snaplineAlbum.title,
                                    coverPhoto, snaplineAlbum.id, RemoteType.SNAPLINE, updatedAt));
                        }
                        callback.onAlbumsLoaded(albums);
                    } else {
                        callback.onDataNotAvailable();
                    }
                } else {
                    try {
                        ResponseBody error = response.errorBody();
                        if (error != null) {
                            Timber.e("Failed to load collections! error: %s",
                                    error.string());
                        } else {
                            Timber.e("Failed to load collections! error: null");
                        }
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    callback.onDataNotAvailable();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SnaplineAlbum>> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    private long parseTimestamp(String timestamp) {
        // Timestamps come in format that Android can't parse by default - 2016-07-10T11:00:01-05:00
        // Support for 'X' timezone field is added in Android 24+
        // "2015-08-03T21:33:54.800632Z"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
        long updatedAt = 0;
        try {
            updatedAt = format.parse(timestamp.substring(0, timestamp.length() - 1)).getTime();
        } catch (ParseException e) {
            Timber.e(e);
        }
        return updatedAt;
    }

    @Override
    public void getAlbumPhotos(final Album album, int page, final Callbacks.GetAlbumPhotosCallback callback) {
        if (page > 1) {
            callback.onDataNotAvailable();
            return;
        }

        SnaplineAPI api = getApiClient();

        api.getAlbumPhotos(album.getRemoteId()).enqueue(new Callback<List<Media>>() {
            @Override
            public void onResponse(@NonNull Call<List<Media>> call,
                                   @NonNull Response<List<Media>> response) {
                List<Media> snaplinePhotos = response.body();
                if (response.isSuccessful()) {
                    if (snaplinePhotos != null && snaplinePhotos.size() > 0) {
                        List<Photo> photos = new ArrayList<>();

                        for (Media unsplashPhoto : snaplinePhotos) {
                            photos.add(new Photo(album, unsplashPhoto.url, 800,
                                    600, unsplashPhoto.id));
                        }

                        callback.onAlbumPhotosLoaded(photos);
                    } else {
                        callback.onDataNotAvailable();
                    }
                } else {
                    try {
                        ResponseBody error = response.errorBody();
                        if (error != null) {
                            Timber.e("Failed to load collection photos! error: %s",
                                    error.string());
                        } else {
                            Timber.e("Failed to load collection photos! error: null");
                        }
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    callback.onDataNotAvailable();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Media>> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    private SnaplineAPI getApiClient() {
        OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(
                new OkHttpClient.Builder());

        builder.interceptors().add(new SnaplineAuthInterceptor());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        return retrofit.create(SnaplineAPI.class);
    }

    interface SnaplineAPI {
        @GET("albums")
        Call<List<SnaplineAlbum>> getAlbums();

        @GET("albums/{id}")
        Call<SnaplineAlbum> getAlbum(@Path("id") String albumId);

        @GET("albums/{id}/media")
        Call<List<Media>> getAlbumPhotos(@Path("id") String albumId);
    }

    static class SnaplineAlbum {
        @SerializedName("public_id")
        public final String id;
        public final String title;

        @SerializedName("cover")
        public final Media coverPhoto;

        @SerializedName("updated_at")
        public final String updatedAt;

        public final List<Media> media;

        public SnaplineAlbum(String id, String title, Media coverPhoto, String updatedAt, List<Media> media) {
            this.id = id;
            this.title = title;
            this.coverPhoto = coverPhoto;
            this.updatedAt = updatedAt;
            this.media = media;
        }
    }

    static class Media {
        @SerializedName("public_id")
        public final String id;
        public final String url;

        public Media(String id, String url) {
            this.id = id;
            this.url = url;
        }
    }

    private class SnaplineAuthInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            String token = "Token " + getAuthToken();
            Request newRequest = chain.request().newBuilder()
                    .header("Authorization", token).build();
            return chain.proceed(newRequest);
        }
    }
}
