package com.grunskis.albumone.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.source.local.DbHelper;
import com.grunskis.albumone.gallery.GalleryActivity;

import org.parceler.Parcels;

import java.io.File;

import timber.log.Timber;

public class AlbumOneWidget extends AppWidgetProvider {
    private static final String PREFS_NAME = "com.grunskis.albumone.AlbumOneWidget";
    private static final String PREFS_KEY_PREFIX = "album_id_";

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                    int appWidgetId, Album album) {
        Timber.i("Updating widget id: %d albumId: %s", appWidgetId, album.getId());

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_album_one);

        // TODO: 4/6/2018 use glide to load and save cache the bitmap
        File file = new File(album.getCoverPhoto().getDownloadPath());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        views.setImageViewBitmap(R.id.iv_album_cover_photo, bitmap);

        // taping the image will open the album
        Intent intent1 = new Intent(context, AlbumDetailActivity.class);
        intent1.setAction("openAlbum_" + appWidgetId);
        intent1.putExtra(AlbumDetailActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent1.putExtra(AlbumDetailActivity.EXTRA_LOCAL_ONLY, true);
        PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 0, intent1, 0);
        views.setOnClickPendingIntent(R.id.iv_album_cover_photo, pendingIntent1);

        // taping the play button will start slideshow
        Intent intent2 = new Intent(context, GalleryActivity.class);
        intent2.setAction("startSlideshow_" + appWidgetId);
        intent2.putExtra(GalleryActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent2.putExtra(GalleryActivity.EXTRA_IS_SLIDESHOW, true);
        PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 1, intent2, 0);
        views.setOnClickPendingIntent(R.id.ib_slideshow, pendingIntent2);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void saveWidgetData(Context context, int appWidgetId, long albumId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREFS_KEY_PREFIX + appWidgetId, albumId);
        prefs.apply();
    }

    public static Album loadAlbum(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        long albumId = -1;
        if (prefs.contains(PREFS_KEY_PREFIX + appWidgetId)) {
            albumId = prefs.getLong(PREFS_KEY_PREFIX + appWidgetId, -1);
        }
        if (albumId == -1) {
            return null;
        }
        Album album = DbHelper.getAlbumById(context, albumId);
        album.getCoverPhoto().refreshFromDb(context);
        return album;
    }

    static void deleteWidgetData(Context context, int appWidgetId) {
        Timber.i("Deleting widget id: %d", appWidgetId);
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREFS_KEY_PREFIX + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Album album = loadAlbum(context, appWidgetId);
            Timber.i("Widget album loaded %s appWidgetId: %d", album, appWidgetId);
            if (album != null) {
                updateWidget(context, appWidgetManager, appWidgetId, album);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            deleteWidgetData(context, appWidgetId);
        }
    }
}
