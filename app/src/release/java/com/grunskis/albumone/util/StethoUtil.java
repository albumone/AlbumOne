package com.grunskis.albumone.util;

import okhttp3.OkHttpClient;

public class StethoUtil {
    public static OkHttpClient.Builder addNetworkInterceptor(OkHttpClient.Builder builder) {
        // no need for extra interceptors
        return builder;
    }
}
