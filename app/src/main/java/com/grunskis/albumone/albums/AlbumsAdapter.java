package com.grunskis.albumone.albums;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grunskis.albumone.DisplayHelpers;
import com.grunskis.albumone.GlideApp;
import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {
    private List<Album> mAlbums;
    private AlbumsClickListener mAlbumClickListener;
    private int mDisplayWidth;
    private Context mContext;

    AlbumsAdapter(Context context, AlbumsClickListener albumClickListener) {
        mContext = context;
        mAlbumClickListener = albumClickListener;

        mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    void addAlbums(List<Album> albums) {
        if (mAlbums == null) {
            mAlbums = new ArrayList<>(albums);
        } else {
            mAlbums.addAll(albums);
        }
        notifyDataSetChanged();
    }

    void updateAlbum(Album album) {
        if (mAlbums == null) {
            return;
        }

        for (int i = 0; i < mAlbums.size(); i++) {
            Album a = mAlbums.get(i);
            if (a != null && a.getId() != null && a.getId().equals(album.getId())) {
                mAlbums.set(i, album);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public List<Album> getAlbums() {
        return mAlbums;
    }

    void setAlbums(List<Album> albums) {
        mAlbums = new ArrayList<>(albums);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final AlbumsAdapter.ViewHolder holder, int position) {
        final Album album = mAlbums.get(position);
        Photo coverPhoto = album.getCoverPhoto();

        holder.itemView.getLayoutParams().height = DisplayHelpers.calculateOptimalPhotoHeight(
                mDisplayWidth, coverPhoto);

        Uri uri;
        String localPath = coverPhoto.getDownloadPath();
        if (localPath != null && localPath.length() > 0) {
            uri = Uri.fromFile(new File(localPath));
        } else {
            uri = coverPhoto.getSmallUri();
        }
        GlideApp.with(mContext).load(uri).into(holder.mCoverPhoto);

        holder.mTitle.setText(album.getTitle());

        if (album.getDownloadState() == Album.DownloadState.DOWNLOADING) {
            holder.mDownloadProgress.setVisibility(View.VISIBLE);
        } else if (album.getDownloadState() == Album.DownloadState.DOWNLOADED) {
            holder.mDownloadProgress.setVisibility(View.INVISIBLE);
        }
        if (album.isLocal()) {
            if (album.getRemoteType() == RemoteType.GOOGLE_PHOTOS) {
                holder.mBackendLogo.setImageResource(R.drawable.ic_google_photos_24dp);
            } else if (album.getRemoteType() == RemoteType.UNSPLASH) {
                holder.mBackendLogo.setImageResource(R.drawable.ic_unsplash_24dp);
            }
            holder.mBackendLogo.setVisibility(View.VISIBLE);
        }

        holder.mCoverPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (album.getDownloadState() == Album.DownloadState.DOWNLOADING) {
                    Snackbar.make(view,
                            view.getResources().getString(R.string.album_downloading),
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    mAlbumClickListener.onAlbumClick(album);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mAlbums == null) {
            return 0;
        } else {
            return mAlbums.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView mCoverPhoto;
        final TextView mTitle;
        final ImageView mBackendLogo;
        final ProgressBar mDownloadProgress;

        ViewHolder(View itemView) {
            super(itemView);

            mCoverPhoto = itemView.findViewById(R.id.iv_cover_photo);
            mTitle = itemView.findViewById(R.id.tv_title);
            mBackendLogo = itemView.findViewById(R.id.iv_backend_type);
            mDownloadProgress = itemView.findViewById(R.id.pb_album_downoading);
        }
    }
}
