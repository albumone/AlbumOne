package com.grunskis.albumone.data.source;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import com.grunskis.albumone.data.Album;

import java.util.List;

public class AlbumsLoader extends AsyncTaskLoader<List<Album>> {

    private AlbumsRepository mRepository;

    public AlbumsLoader(@NonNull Context context, AlbumsRepository repository) {
        super(context);

        mRepository = repository;
    }

    @Nullable
    @Override
    public List<Album> loadInBackground() {
        return null; //mRepository.getAlbums();
    }
}
