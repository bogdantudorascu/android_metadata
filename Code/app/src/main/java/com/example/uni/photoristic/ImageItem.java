package com.example.uni.photoristic;


import java.io.File;


/**
 * Class used to represent the structure of an image item
 */
public class ImageItem {
    File imageFile;
    public ImageItem(File imageFile){
        this.imageFile = imageFile;
    }

    /**
     * Method to get the file representing an image
     * @return File of the image
     */
    public File getImageFile() {
        return imageFile;
    }
}
