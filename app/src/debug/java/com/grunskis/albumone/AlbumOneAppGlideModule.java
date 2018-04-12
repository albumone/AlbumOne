package com.grunskis.albumone;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import okhttp3.OkHttpClient;

@GlideModule
public class AlbumOneAppGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {

        super.registerComponents(context, glide, registry);

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new com.facebook.stetho.okhttp3.StethoInterceptor())
                .build();

        registry.append(GlideUrl.class, InputStream.class,
                new com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader.Factory(client));
    }
}
