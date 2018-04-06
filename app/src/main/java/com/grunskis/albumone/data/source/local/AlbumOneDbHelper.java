package com.grunskis.albumone.data.source.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AlbumOneDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "albumone.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_ALBUMS_TABLE =
            "CREATE TABLE " + AlbumOnePersistenceContract.AlbumEntry.TABLE_NAME + " (" +
                    AlbumOnePersistenceContract.AlbumEntry._ID + " INTEGER PRIMARY KEY," +
                    AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_TITLE + " TEXT," +
                    AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_COVER_PHOTO_ID + " INTEGER," +
                    AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_ID + " TEXT," +
                    AlbumOnePersistenceContract.AlbumEntry.COLUMN_NAME_REMOTE_TYPE + " INTEGER)";

    private static final String SQL_CREATE_PHOTOS_TABLE =
            "CREATE TABLE " + AlbumOnePersistenceContract.PhotoEntry.TABLE_NAME + " (" +
                    AlbumOnePersistenceContract.PhotoEntry._ID + " INTEGER PRIMARY KEY," +
                    AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_ALBUM_ID + " INTEGER," +
                    AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_URL + " TEXT," +
                    AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_WIDTH + " INTEGER," +
                    AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_HEIGHT + " INTEGER," +
                    AlbumOnePersistenceContract.PhotoEntry.COLUMN_NAME_REMOTE_ID + " TEXT)";

    private static final String SQL_CREATE_DOWNLOADS_TABLE =
            "CREATE TABLE " + AlbumOnePersistenceContract.DownloadEntry.TABLE_NAME + " (" +
                    AlbumOnePersistenceContract.DownloadEntry._ID + " INTEGER PRIMARY KEY," +
                    AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_ALBUM_REMOTE_ID + " TEXT," +
                    AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_STARTED_AT + " INTEGER," +
                    AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_FINISHED_AT + " INTEGER," +
                    AlbumOnePersistenceContract.DownloadEntry.COLUMN_NAME_FAILED_AT + " INTEGER)";

    public AlbumOneDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ALBUMS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PHOTOS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_DOWNLOADS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {}
}
