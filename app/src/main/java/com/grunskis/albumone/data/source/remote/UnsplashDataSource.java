package com.grunskis.albumone.data.source.remote;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.Callbacks;
import com.grunskis.albumone.data.source.RemoteDataSource;
import com.grunskis.albumone.util.StethoUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
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
    private static final int REGULAR_PHOTO_WIDTH = 1080;

    private static UnsplashDataSource INSTANCE = null;

    private String mAuthToken;

    private UnsplashDataSource() {
    }

    public static UnsplashDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UnsplashDataSource();
        }
        return INSTANCE;
    }

    private int calcPhotoHeight(int fullWidth, int fullHeight) {
        float ratio = (float) fullWidth / REGULAR_PHOTO_WIDTH;
        return round((float) fullHeight / ratio);
    }

    private UnsplashApi getApiClient() {
        // TODO: 2/6/2018 figure out where to move this initialization code
        OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(new OkHttpClient.Builder());
        builder.interceptors().add(new UnsplashAuthInterceptor());
        OkHttpClient client = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.unsplash.com/") // TODO: 2/7/2018 move to config or db
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return retrofit.create(UnsplashApi.class);
    }

    @Override
    public void setAuthToken(String authToken) {
        mAuthToken = authToken;
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
                                albums.add(new Album(collection.title, coverPhoto, collection.id,
                                        RemoteType.UNSPLASH));
                            }
                            callback.onAlbumsLoaded(albums);
                        } else {
                            callback.onDataNotAvailable();
                        }
                    } else {
                        try {
                            Timber.e("Failed to load collections: %s", response.errorBody().string());
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

    @Override
    public void getAlbumPhotos(final Album album, int page, final Callbacks.GetAlbumPhotosCallback callback) {
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
                        Timber.e("Failed to load collection photos: %s", response.errorBody().string());
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    // TODO: 3/22/2018 add error callback
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

    interface UnsplashApi {
        @GET("collections/featured")
        Call<List<Collection>> getCollections(@Query("page") int page);

        @GET("collections/{id}/photos")
        Call<List<UnsplashPhoto>> getCollectionPhotos(@Path("id") String collectionId,
                                                      @Query("page") int page);
    }

    static class Urls {
        public final String regular;
        public final String small;

        public Urls(String regular, String small) {
            this.regular = regular;
            this.small = small;
        }
    }

    static class Collection {
        public final String id;
        public final String title;

        @SerializedName("cover_photo")
        public final UnsplashPhoto coverPhoto;

        @SerializedName("total_photos")
        public final int totalPhotos;

        public Collection(String id, String title, UnsplashPhoto coverPhoto, int totalPhotos) {
            this.id = id;
            this.title = title;
            this.coverPhoto = coverPhoto;
            this.totalPhotos = totalPhotos;
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

    private class UnsplashAuthInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            String clientId = "Client-ID " + mAuthToken;
            Request newRequest = chain.request().newBuilder().header("Authorization", clientId).build();
            return chain.proceed(newRequest);
        }
    }
}
