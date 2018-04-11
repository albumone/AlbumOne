package com.grunskis.albumone.data.source;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.source.local.DbHelper;

import org.parceler.Parcels;

import java.util.List;

import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private Context mContext;
    private ContentResolver mContentResolver;

    SyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    private SyncAdapter(Context context, boolean autoInitialize, boolean parallelSync) {
        super(context, autoInitialize, parallelSync);

        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Timber.i("onPerformSync() starting...");

        List<Album> albums = DbHelper.getAlbums(mContentResolver);
        for (Album album : albums) {
            Intent intent = new Intent(mContext, DownloadService.class);
            intent.putExtra(DownloadService.EXTRA_ALBUM, Parcels.wrap(album));
            intent.putExtra(DownloadService.EXTRA_IS_REFRESH, true);
            mContext.startService(intent);
        }

        Timber.i("onPerformSync() finished.");
    }
}
