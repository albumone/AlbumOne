package com.grunskis.albumone.albums;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.R;
import com.grunskis.albumone.albumdetail.AlbumDetailActivity;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
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

        if (savedInstanceState != null) {
            Timber.w("yoooooooo");
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

            // TODO: 3/23/2018 fix of by "one" issue where photo apears to be a bit smaller that the display widht
            // set height of the photo so that the width matches display width
            float aspectRatio = (float) mDisplayWidth / (float) coverPhoto.getWidth();
            holder.itemView.getLayoutParams().height = (int) (coverPhoto.getHeight() * aspectRatio);

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

            holder.mCoverPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Timber.d("Album clicked %s", album.getTitle());
                    mAlbumClickListener.onAlbumClick(album);
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

            ViewHolder(View itemView) {
                super(itemView);

                mCoverPhoto = itemView.findViewById(R.id.iv_cover_photo);
                mTitle = itemView.findViewById(R.id.tv_title);
            }
        }
    }
}
