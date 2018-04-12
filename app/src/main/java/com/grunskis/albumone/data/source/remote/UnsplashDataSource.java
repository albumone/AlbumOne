package com.grunskis.albumone.data.source.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.grunskis.albumone.TLSSocketFactory;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.util.StethoUtil;

import java.io.IOException;
import java.security.KeyStore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.TlsVersion;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import timber.log.Timber;

import static java.lang.Math.round;

public class UnsplashDataSource implements RemoteDataSource {
    private static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN_UNSPLASH";

    private static final int REGULAR_PHOTO_WIDTH = 1080;
    private static final String API_BASE_URL = "https://api.unsplash.com/";
    private static final String API_VERSION = "v1";

    private static UnsplashDataSource INSTANCE = null;

    private String mAuthToken;
    private SharedPreferences mSharedPreferences;

    private UnsplashDataSource(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String authToken = getAuthToken();
        if (authToken != null) {
            setAuthToken(authToken);
        }
    }

    public static UnsplashDataSource getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UnsplashDataSource(context);
        }
        return INSTANCE;
    }

    private static OkHttpClient.Builder enableTls12(OkHttpClient.Builder client) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            client.sslSocketFactory(new TLSSocketFactory(), trustManager);
            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1)
                    .build();
            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(cs);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);
            client.connectionSpecs(specs);
        } catch (Exception exc) {
            Timber.e(exc);
        }
        return client;
    }

    private static long parseTimestamp(String timestamp) {
        // Timestamps come in format that Android can't parse by default - 2016-07-10T11:00:01-05:00
        // Support for 'X' timezone field is added in Android 24+
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        long updatedAt = 0;
        try {
            StringBuilder sb = new StringBuilder(timestamp);
            int i = timestamp.lastIndexOf(":");
            sb.replace(i, i + 1, "");
            updatedAt = format.parse(sb.toString()).getTime();
        } catch (ParseException e) {
            Timber.e(e);
        }
        return updatedAt;
    }

    private static int calcPhotoHeight(int fullWidth, int fullHeight) {
        float ratio = (float) fullWidth / REGULAR_PHOTO_WIDTH;
        return round((float) fullHeight / ratio);
    }

    private String getAuthToken() {
        return mSharedPreferences.getString(PREF_AUTH_TOKEN, null);
    }

    @Override
    public void setAuthToken(String authToken) {
        mSharedPreferences.edit().putString(PREF_AUTH_TOKEN, authToken).apply();
        mAuthToken = authToken;
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthToken() != null;
    }

    @Override
    public void getAlbumPhotos(final Album album, int page,
                               final Callbacks.GetAlbumPhotosCallback callback) {
        UnsplashApi api = getApiClient();

        api.getCollectionPhotos(album.getRemoteId(), page).enqueue(new Callback<List<UnsplashPhoto>>() {
            @Override
            public void onResponse(@NonNull Call<List<UnsplashPhoto>> call,
                                   @NonNull Response<List<UnsplashPhoto>> response) {
                List<UnsplashPhoto> unsplashPhotos = response.body();
                if (response.isSuccessful()) {
                    if (unsplashPhotos != null && unsplashPhotos.size() > 0) {
                        List<Photo> photos = new ArrayList<>();

                        for (UnsplashPhoto unsplashPhoto : unsplashPhotos) {
                            // TODO: 3/21/2018 try fetching custom size thumbnail to avoid calculating height ourselves?
                            int height = calcPhotoHeight(unsplashPhoto.width, unsplashPhoto.height);
                            photos.add(new Photo(album, unsplashPhoto.urls.regular, REGULAR_PHOTO_WIDTH,
                                    height, unsplashPhoto.id));
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
            public void onFailure(@NonNull Call<List<UnsplashPhoto>> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    @Override
    public void getAlbum(String remoteId, final Callbacks.GetAlbumCallback callback) {
        UnsplashApi api = getApiClient();

        api.getCollection(remoteId).enqueue(new Callback<Collection>() {
            @Override
            public void onResponse(@NonNull Call<Collection> call,
                                   @NonNull Response<Collection> response) {
                if (response.isSuccessful()) {
                    Collection collection = response.body();
                    if (collection != null) {
                        int height = calcPhotoHeight(collection.coverPhoto.width,
                                collection.coverPhoto.height);
                        Photo coverPhoto = new Photo(null,
                                collection.coverPhoto.urls.regular, REGULAR_PHOTO_WIDTH,
                                height, collection.coverPhoto.id);
                        long updatedAt = parseTimestamp(collection.updatedAt);

                        callback.onAlbumLoaded(new Album(collection.title, coverPhoto,
                                collection.id, RemoteType.UNSPLASH, updatedAt));
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
            public void onFailure(@NonNull Call<Collection> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    @Override
    public void getAlbums(int page, final Callbacks.GetAlbumsCallback callback) {
        UnsplashApi api = getApiClient();

        api.getCollections(page).enqueue(new Callback<List<Collection>>() {
            @Override
            public void onResponse(@NonNull Call<List<Collection>> call,
                                   @NonNull Response<List<Collection>> response) {

                List<Collection> collections = response.body();
                if (response.isSuccessful()) {
                    if (collections != null && collections.size() > 0) {
                        List<Album> albums = new ArrayList<>();

                        for (Collection collection : collections) {
                            Timber.i("Collection title: %s totalPhotos: %d",
                                    collection.title, collection.totalPhotos);

                            int height = calcPhotoHeight(collection.coverPhoto.width,
                                    collection.coverPhoto.height);
                            Photo coverPhoto = new Photo(null,
                                    collection.coverPhoto.urls.regular, REGULAR_PHOTO_WIDTH,
                                    height, collection.coverPhoto.id);
                            long updatedAt = parseTimestamp(collection.updatedAt);

                            albums.add(new Album(collection.title, coverPhoto, collection.id,
                                    RemoteType.UNSPLASH, updatedAt));
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
            public void onFailure(@NonNull Call<List<Collection>> call, @NonNull Throwable t) {
                Timber.e(t);
                callback.onDataNotAvailable();
            }
        });
    }

    interface UnsplashApi {
        @GET("collections/featured")
        Call<List<Collection>> getCollections(@Query("page") int page);

        @GET("collections/{id}/photos")
        Call<List<UnsplashPhoto>> getCollectionPhotos(@Path("id") String collectionId,
                                                      @Query("page") int page);

        @GET("collections/{id}")
        Call<Collection> getCollection(@Path("id") String collectionId);
    }

    static class Urls {
        public final String regular;

        public Urls(String regular) {
            this.regular = regular;
        }
    }

    static class UnsplashPhoto {
        public final Urls urls;
        public String id;
        public int width;
        public int height;

        public UnsplashPhoto(String id, Urls urls, int width, int height) {
            this.id = id;
            this.urls = urls;
            this.width = width;
            this.height = height;
        }
    }

    static class Collection {
        public final String id;
        public final String title;

        @SerializedName("cover_photo")
        public final UnsplashPhoto coverPhoto;

        @SerializedName("total_photos")
        public final int totalPhotos;

        @SerializedName("updated_at")
        public final String updatedAt;

        public Collection(String id, String title, UnsplashPhoto coverPhoto, int totalPhotos,
                          String updatedAt) {
            this.id = id;
            this.title = title;
            this.coverPhoto = coverPhoto;
            this.totalPhotos = totalPhotos;
            this.updatedAt = updatedAt;
        }
    }

    private class UnsplashAuthInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            String clientId = "Client-ID " + mAuthToken;
            Request newRequest = chain.request().newBuilder().header("Authorization", clientId).build();
            return chain.proceed(newRequest);
        }
    }

    private UnsplashApi getApiClient() {
        OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(new OkHttpClient.Builder());

        // fixing TLS handshake issue as described here https://github.com/santhoshvai/Evlo/issues/2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            builder = enableTls12(builder);
        }

        builder.interceptors().add(new UnsplashAuthInterceptor());
        builder.interceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
                Request.Builder requestBuilder = chain.request().newBuilder();
                requestBuilder.addHeader("Accept-Version", API_VERSION);
                return chain.proceed(requestBuilder.build());
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        return retrofit.create(UnsplashApi.class);
    }
}
