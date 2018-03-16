package com.grunskis.albumone.albumdetail;

import com.grunskis.albumone.data.Photo;

import java.util.List;

public interface PhotoClickListener {
    void onClick(List<Photo> photos, int position);
}
