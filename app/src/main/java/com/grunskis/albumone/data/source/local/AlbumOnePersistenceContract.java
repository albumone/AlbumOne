package com.grunskis.albumone.data.source.local;

import android.net.Uri;
import android.provider.BaseColumns;

import com.grunskis.albumone.BuildConfig;

public class AlbumOnePersistenceContract {
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final String CONTENT_DOWNLOADS_TYPE = "vnd.android.cursor.dir/" +
            CONTENT_AUTHORITY + "/" + DownloadEntry.TABLE_NAME;
    private static final String CONTENT_SCHEME = "content://";

    public static final String CONTENT_ALBUMS_TYPE = "vnd.android.cursor.dir/" +
            CONTENT_AUTHORITY + "/" + AlbumEntry.TABLE_NAME;
    public static final String CONTENT_ALBUM_TYPE = "vnd.android.cursor.item/" +
            CONTENT_AUTHORITY + "/" + AlbumEntry.TABLE_NAME;

    public static final String CONTENT_PHOTOS_TYPE = "vnd.android.cursor.dir/" +
            CONTENT_AUTHORITY + "/" + PhotoEntry.TABLE_NAME;
    private static final Uri BASE_CONTENT_URI = Uri.parse(CONTENT_SCHEME + CONTENT_AUTHORITY);

    // private constructor to prevent instantiation
    private AlbumOnePersistenceContract() {}

    public static abstract class AlbumEntry implements BaseColumns {
        public static final String TABLE_NAME = "albums";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI
                .buildUpon()
                .appendPath(TABLE_NAME)
                .build();

        public static final String COLUMN_NAME_TITLE = "title";
        // TODO: 3/22/2018 cover_photo_id
        public static final String COLUMN_NAME_COVER_PHOTO_PATH = "cover_photo_path";
        public static final String COLUMN_NAME_COVER_PHOTO_WIDTH = "cover_photo_width";
        public static final String COLUMN_NAME_COVER_PHOTO_HEIGHT = "cover_photo_height";
        public static final String COLUMN_NAME_COVER_PHOTO_REMOTE_ID = "cover_photo_remote_id";
        public static final String COLUMN_NAME_REMOTE_ID = "remote_id";
        public static final String COLUMN_NAME_REMOTE_TYPE = "remote_type";

        public static String[] ALBUM_COLUMNS = new String[] {
                AlbumEntry._ID,
                AlbumEntry.COLUMN_NAME_TITLE,
                AlbumEntry.COLUMN_NAME_COVER_PHOTO_PATH,
                AlbumEntry.COLUMN_NAME_COVER_PHOTO_WIDTH,
                AlbumEntry.COLUMN_NAME_COVER_PHOTO_HEIGHT,
                AlbumEntry.COLUMN_NAME_COVER_PHOTO_REMOTE_ID,
                AlbumEntry.COLUMN_NAME_REMOTE_ID,
                AlbumEntry.COLUMN_NAME_REMOTE_TYPE,
        };

        public static Uri buildUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }
    }

    public static abstract class PhotoEntry implements BaseColumns {
        public static final String TABLE_NAME = "photos";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI
                .buildUpon()
                .appendPath(TABLE_NAME)
                .build();

        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_ALBUM_ID = "album_id";
        public static final String COLUMN_NAME_WIDTH = "width";
        public static final String COLUMN_NAME_HEIGHT = "height";
        public static final String COLUMN_NAME_REMOTE_ID = "remote_id";

        public static String[] PHOTO_COLUMNS = new String[]{
                PhotoEntry._ID,
                PhotoEntry.COLUMN_NAME_URL,
                PhotoEntry.COLUMN_NAME_ALBUM_ID,
                PhotoEntry.COLUMN_NAME_WIDTH,
                PhotoEntry.COLUMN_NAME_HEIGHT,
                PhotoEntry.COLUMN_NAME_REMOTE_ID,
        };

        public static Uri buildUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }
    }

    public static abstract class DownloadEntry implements BaseColumns {
        public static final String TABLE_NAME = "downloads";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI
                .buildUpon()
                .appendPath(TABLE_NAME)
                .build();

        public static final String COLUMN_NAME_ALBUM_REMOTE_ID = "album_remote_id";
        public static final String COLUMN_NAME_STARTED_AT = "started_at";
        public static final String COLUMN_NAME_FINISHED_AT = "finished_at";
        public static final String COLUMN_NAME_FAILED_AT = "failed_at";

        public static String[] COLUMNS = new String[]{
                DownloadEntry._ID,
                DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID,
                DownloadEntry.COLUMN_NAME_STARTED_AT,
                DownloadEntry.COLUMN_NAME_FINISHED_AT,
                DownloadEntry.COLUMN_NAME_FAILED_AT,
        };

        public static Uri buildUriWith(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
    }
}
