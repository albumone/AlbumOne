package com.grunskis.albumone.albums;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grunskis.albumone.DisplayHelpers;
import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.RemoteType;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.util.StethoUtil;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class AlbumsFragment extends Fragment implements AlbumsContract.View, AlbumClickListener {
    static final String ARGUMENT_LOCAL_ONLY = "ARGUMENT_LOCAL_ONLY";

    private ProgressBar mLoading;

    private AlbumsContract.Presenter mPresenter;
    private AlbumsAdapter mAlbumsAdapter;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    private boolean mShowOnlyLocalAlbums = false;

    public AlbumsFragment() {}

    public static AlbumsFragment newInstance(boolean showOnlyLocalAlbums) {
        AlbumsFragment albumsFragment = new AlbumsFragment();

        Bundle args = new Bundle();
        args.putBoolean(ARGUMENT_LOCAL_ONLY, showOnlyLocalAlbums);
        albumsFragment.setArguments(args);

        return albumsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mShowOnlyLocalAlbums = args.getBoolean(ARGUMENT_LOCAL_ONLY, false);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Album album = Parcels.unwrap(intent.getParcelableExtra(DownloadService.EXTRA_ALBUM));
                mPresenter.onAlbumDownloaded(album);
            }
        };

        Activity activity = getActivity();
        if (activity != null) {
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(
                    activity.getApplicationContext());
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver,
                    new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_FINISHED));
        }
    }

    @Override
    public void onDestroy() {
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        mLoading = rootView.findViewById(R.id.pb_loading);

        RecyclerView recyclerView = rootView.findViewById(R.id.rv_albums);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        if (!mShowOnlyLocalAlbums) {
            recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                    Timber.d("onLoadMore() page: %d totalItems: %d", page, totalItemsCount);
                    mPresenter.loadAlbums(page + 1);
                }
            });
        }

        mAlbumsAdapter = new AlbumsAdapter(getContext(), this);
        recyclerView.setAdapter(mAlbumsAdapter);

        return rootView;
    }

    @Override
    public void resetAlbums() {
        mAlbumsAdapter.resetAlbums();
    }

    @Override
    public void setPresenter(AlbumsContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        if (active) {
            mLoading.setVisibility(View.VISIBLE);
        } else {
            mLoading.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void showAlbums(List<Album> albums) {
        mAlbumsAdapter.addAlbums(albums);
    }

    public void showAlbumDetails(Album album) {
        Intent intent = new Intent(getContext(), AlbumDetailActivity.class);
        intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(AlbumDetailActivity.EXTRA_LOCAL_ONLY, mShowOnlyLocalAlbums);
        startActivity(intent);
    }

    @Override
    public void onAlbumClick(Album album) {
        mPresenter.openAlbumDetails(album);
    }

    @Override
    public void updateAlbum(Album album) {
        mAlbumsAdapter.updateAlbum(album);
    }

    private static class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {
        private Picasso mPicasso;
        private List<Album> mAlbums;
        private AlbumClickListener mAlbumClickListener;
        private int mDisplayWidth;

        AlbumsAdapter(Context context, AlbumClickListener albumClickListener) {
            // TODO: 3/23/2018 in local mode no need to instantiate these
            OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(
                    new OkHttpClient.Builder());
            mPicasso = new Picasso.Builder(context)
                    .downloader(new OkHttp3Downloader(builder.build()))
                    .loggingEnabled(true)
                    .indicatorsEnabled(true) // TODO: 3/19/2018 enable in debug only
                    .build();

            mAlbumClickListener = albumClickListener;

            mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        }

        void resetAlbums() {
            if (mAlbums != null && mAlbums.size() > 0) {
                mAlbums.clear();
                notifyDataSetChanged();
            }
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
            for (int i = 0; i < mAlbums.size(); i++) {
                Album a = mAlbums.get(i);
                if (a != null && a.getId() != null && a.getId().equals(album.getId())) {
                    mAlbums.set(i, album);
                    notifyItemChanged(i);
                    break;
                }
            }
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

            String localPath = coverPhoto.getDownloadPath();
            if (localPath != null && localPath.length() > 0) {
                File file = new File(localPath);
                if (file.exists()) {
                    holder.mCoverPhoto.setImageURI(Uri.fromFile(file));
                }
            } else {
                mPicasso.load(coverPhoto.getSmallUri()).into(holder.mCoverPhoto);
            }

            holder.mTitle.setText(album.getTitle());

            if (album.getDownloadState() == Album.DownloadState.DOWNLOADING) {
                holder.mDownloadProgress.setVisibility(View.VISIBLE);
            } else if (album.getDownloadState() == Album.DownloadState.DOWNLOADED) {
                holder.mDownloadProgress.setVisibility(View.INVISIBLE);

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
}
