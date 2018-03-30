package com.grunskis.albumone.albumdetail;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.grunskis.albumone.DisplayHelpers;
import com.grunskis.albumone.EndlessRecyclerViewScrollListener;
import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Album;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.data.source.DownloadService;
import com.grunskis.albumone.gallery.GalleryActivity;
import com.grunskis.albumone.util.StethoUtil;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class AlbumDetailFragment extends Fragment implements AlbumDetailContract.View,
        PhotoClickListener {

    private static final String ARGUMENT_ALBUM = "ALBUM";

    private AlbumDetailContract.Presenter mPresenter;
    private PhotosAdapter mPhotosAdapter;
    private ProgressBar mLoading;
    private Album mAlbum;
    private int mMenuItemId = -1;
    private MenuItem mDownloadMenuItem;
    private MenuItem mDoneMenuItem;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    public static AlbumDetailFragment newInstance(Album album) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ALBUM, Parcels.wrap(album));
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            mAlbum = Parcels.unwrap(args.getParcelable(ARGUMENT_ALBUM));
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_album_detail, container, false);

        mLoading = rootView.findViewById(R.id.pb_loading);

        RecyclerView recyclerView = rootView.findViewById(R.id.rv_photos);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        if (!mAlbum.isLocal()) {
            recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                    Timber.d("onLoadMore() page: %d totalItems: %d", page, totalItemsCount);
                    mPresenter.loadAlbumPhotos(page + 1);
                }
            });
        }

        mPhotosAdapter = new PhotosAdapter(getContext(), this);
        recyclerView.setAdapter(mPhotosAdapter);

        return rootView;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                mPresenter.downloadAlbum();
                return true;

            case R.id.action_done:
                mPresenter.showAlbumDownloaded();
                return true;

            case R.id.action_slideshow:
                mPresenter.startSlideshow(mPhotosAdapter.getPhotos());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mMenuItemId > -1) {
            menu.findItem(mMenuItemId).setVisible(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_album_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);

        mDownloadMenuItem = menu.findItem(R.id.action_download);
        mDoneMenuItem = menu.findItem(R.id.action_done);
    }

    @Override
    public void setPresenter(AlbumDetailContract.Presenter presenter) {
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
    public void setDownloadIndicator(boolean downloading) {
        AlbumDetailActivity activity = ((AlbumDetailActivity) getActivity());
        if (activity == null) return;

        if (downloading) {
            activity.showDownloadProgressBar();
        } else {
            activity.hideDownloadProgressBar();
        }
    }

    public void showAlbumPhotos(List<Photo> photos) {
        mPhotosAdapter.addPhotos(photos);
    }

    @Override
    public void resetAlbumPhotos() {
        mPhotosAdapter.resetAlbumPhotos();
    }

    public void showAlbumDownloadStarted() {
        if (mDownloadMenuItem != null) {
            mDownloadMenuItem.setVisible(false);
        }

        Activity activity = getActivity();
        if (activity != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    getResources().getString(R.string.album_download_started),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void showAlbumDownloadFinished() {
        if (mDoneMenuItem != null) {
            mDoneMenuItem.setVisible(true);
        }

        Activity activity = getActivity();
        if (activity != null) {
            Snackbar.make(activity.findViewById(android.R.id.content),
                    getResources().getString(R.string.album_download_finished),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    public void showAlbumDownloaded() {
        Activity activity = getActivity();
        if (activity != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    getResources().getString(R.string.album_downloaded),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(List<Photo> photos, int position) {
        mPresenter.openPhotoGallery(mAlbum, photos, position);
    }

    public void showPhotoGallery(Album album, List<Photo> photos, int position) {
        Intent intent = new Intent(getContext(), GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(GalleryActivity.EXTRA_PHOTOS, Parcels.wrap(photos));
        intent.putExtra(GalleryActivity.EXTRA_POSITION, position);
        startActivity(intent);
    }

    @Override
    public void openSlideshow(List<Photo> photos) {
        Intent intent = new Intent(getContext(), GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_ALBUM, Parcels.wrap(mAlbum));
        intent.putExtra(GalleryActivity.EXTRA_PHOTOS, Parcels.wrap(photos));
        intent.putExtra(GalleryActivity.EXTRA_IS_SLIDESHOW, true);
        startActivity(intent);
    }

    @Override
    public void startAlbumDownload() {
        Context context = getContext();
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.EXTRA_ALBUM, Parcels.wrap(mAlbum));
        if (context != null) {
            context.startService(intent);
        } else {
            Timber.e("Context is null!");
        }
    }

    @Override
    public void showDoneIcon() {
        mMenuItemId = R.id.action_done;
    }

    @Override
    public void showDownloadIcon() {
        mMenuItemId = R.id.action_download;
    }

    @Override
    public void showSlideshowIcon() {
        mMenuItemId = R.id.action_slideshow;
    }

    private static class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {
        private Picasso mPicasso;
        private List<Photo> mPhotos;
        private int mDisplayWidth;
        private PhotoClickListener mClickListener;

        PhotosAdapter(Context context, PhotoClickListener clickListener) {
            OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(
                    new OkHttpClient.Builder());
            mPicasso = new Picasso.Builder(context)
                    .downloader(new OkHttp3Downloader(builder.build()))
                    .loggingEnabled(true)
                    .indicatorsEnabled(true) // TODO: 3/19/2018 debug only
                    .build();

            mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

            mClickListener = clickListener;
        }

        void addPhotos(List<Photo> photos) {
            if (mPhotos == null) {
                mPhotos = new ArrayList<>(photos);
            } else {
                mPhotos.addAll(photos);
            }
            notifyDataSetChanged();
        }

        void resetAlbumPhotos() {
            if (mPhotos != null) {
                mPhotos.clear();
                notifyDataSetChanged();
            }
        }

        public List<Photo> getPhotos() {
            return mPhotos;
        }

        @NonNull
        @Override
        public PhotosAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_photo, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PhotosAdapter.ViewHolder holder, int position) {
            final Photo photo = mPhotos.get(position);

            // calculate the resulting height of the image manually and set it to the item view
            // to avoid the flaky behaviour when the images are being loaded and resized by picasso
            holder.itemView.getLayoutParams().height = DisplayHelpers.calculateOptimalPhotoHeight(
                    mDisplayWidth, photo);

            String localPath = photo.getDownloadPath();
            if (localPath != null && localPath.length() > 0) {
                File file = new File(photo.getDownloadPath());
                if (file.exists()) {
                    holder.mPhoto.setImageURI(Uri.fromFile(file));
                }
            } else {
                mPicasso.load(photo.getSmallUri()).into(holder.mPhoto);
            }

            holder.mPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onClick(mPhotos, holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mPhotos == null) {
                return 0;
            } else {
                return mPhotos.size();
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView mPhoto;

            ViewHolder(View itemView) {
                super(itemView);

                mPhoto = itemView.findViewById(R.id.iv_photo);
            }
        }
    }
}
