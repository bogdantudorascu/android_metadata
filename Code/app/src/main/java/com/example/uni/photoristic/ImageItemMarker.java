package com.example.uni.photoristic;

import com.google.android.gms.maps.model.Marker;


/**
 * Class used to represent the structure of an image marker
 */
public class ImageItemMarker {
    Marker marker;
    int position;

    public ImageItemMarker(Marker marker, int position){
        this.marker = marker;
        this.position = position;
    }
}
