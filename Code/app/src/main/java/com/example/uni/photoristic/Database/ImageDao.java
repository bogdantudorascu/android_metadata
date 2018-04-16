package com.example.uni.photoristic.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Data access object used to query the Room database
 */
@Dao
public interface ImageDao {


    /**
     * Get an image object from the database
     *
     * @param name Parameter used to identify an ImageData object in the database
     * @return A list(always 1 or 0) of ImageData
     */
    @Query("SELECT * FROM ImageData WHERE name=:name")
    public List<ImageData> findImageByName (String name);

    /**
     * Method used to insert image details into the database
     * @param image The ImageData object to be inserted
     */
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    void insertImageData(ImageData image);

    /**
     * Method used to delete an image from the database
     * @param image The ImageData object to be deleted
     */
    @Delete
    void deleteImage(ImageData image);
}
