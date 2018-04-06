package com.grunskis.albumone.gallery;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;
import com.grunskis.albumone.GlideApp;
import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class GalleryAdapter extends PagerAdapter {
    private Context mContext;
    private List<Photo> mPhotos;
    private View.OnClickListener mClickListener;

    GalleryAdapter(Context context, View.OnClickListener clickListener) {
        mContext = context;
        mClickListener = clickListener;
    }

    public void addPhotos(List<Photo> photos) {
        if (mPhotos == null) {
            mPhotos = new ArrayList<>();
        }
        mPhotos.addAll(photos);

        notifyDataSetChanged();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_gallery_photo,
                container, false);
        PhotoView photoView = layout.findViewById(R.id.photo);

        Photo photo = mPhotos.get(position);

        Uri uri;
        if (photo.getDownloadPath() != null) {
            uri = Uri.fromFile(new File(photo.getDownloadPath()));
        } else {
            uri = photo.getSmallUri();
        }
        GlideApp.with(mContext).load(uri).into(photoView);

        layout.setOnClickListener(mClickListener);

        container.addView(layout);
        return layout;
    }

    @Override
    public int getCount() {
        if (mPhotos == null) {
            return 0;
        } else {
            return mPhotos.size();
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    public List<Photo> getPhotos() {
        return mPhotos;
    }
}
