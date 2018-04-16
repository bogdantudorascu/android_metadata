package com.example.uni.photoristic.Database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * The ImageData object structure
 */
@Entity
public class ImageData {

    @PrimaryKey
    @NonNull
    public String name;

    public String title;
    public String description;
    public String time;

    @Nullable
    public double latitude;
    @Nullable
    public double longitude;


    public ImageData(String name, String title, String description,String time, double latitude, double longitude) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Ignore
    public ImageData(String name, String title, String description, String time) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.time = time;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getName() {
        return title;
    }

    public void setName(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude(){return latitude;}

    public double getLongitude(){return longitude;}

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }
}