package com.grunskis.albumone.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.grunskis.albumone.R;
import com.grunskis.albumone.data.Photo;
import com.grunskis.albumone.util.StethoUtil;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

class GalleryAdapter extends PagerAdapter {
    private Picasso mPicasso;
    private Context mContext;
    private List<Photo> mPhotos;
    private int mDisplayWidth;
    private View.OnClickListener mClickListener;

    GalleryAdapter(Context context, View.OnClickListener clickListener) {
        mContext = context;
        mClickListener = clickListener;

        OkHttpClient.Builder builder = StethoUtil.addNetworkInterceptor(
                new OkHttpClient.Builder());
        mPicasso = new Picasso.Builder(context)
                .downloader(new OkHttp3Downloader(builder.build()))
                .loggingEnabled(true)
                .indicatorsEnabled(true) // TODO: 3/19/2018 enable in debug only
                .build();

        mDisplayWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public void setPhotos(List<Photo> photos) {
        mPhotos = new ArrayList<>(photos);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_gallery_photo, container, false);
        ImageView imageView = layout.findViewById(R.id.photo);

        Photo photo = mPhotos.get(position);
        if (photo.getDownloadPath() != null) {
            File file = new File(photo.getDownloadPath());
            imageView.setImageURI(Uri.fromFile(file));
        } else {
            mPicasso.load(photo.getSmallUri())
                    .resize(mDisplayWidth, 0)
                    .into(imageView);
        }

        layout.setOnClickListener(mClickListener);

        container.addView(layout);
        return layout;
    }

    @Override
    public int getCount() {
        return mPhotos.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object; // TODO: 3/19/2018 is this ok?
    }
}
