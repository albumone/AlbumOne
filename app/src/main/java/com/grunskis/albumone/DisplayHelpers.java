package com.grunskis.albumone;

import com.grunskis.albumone.data.Photo;

import static java.lang.Math.round;

public class DisplayHelpers {
    public static int calculateOptimalPhotoHeight(int displayWidth, Photo photo) {
        float aspectRatio = (float) displayWidth / photo.getWidth();
        return round(photo.getHeight() * aspectRatio);
    }
}
