package com.example.uni.photoristic.Extra;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;

import com.example.uni.photoristic.ImageItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.os.Environment.isExternalStorageRemovable;

public class Common {

    /**
     * Debug Tag for use logging debug output to LogCat
     */
    private static final String TAG = Common.class.getSimpleName();

    /**
     * This method simply creates a file in the external storage to be used for saving the photo
     * @return The file in which the image will be saved
     */
    public static File savePhotoToExternalStorage(){
        File fileStorageDirectory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Photoristic");

        if (!fileStorageDirectory.exists()){
            if (!fileStorageDirectory.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(fileStorageDirectory.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    /**
     * This method will cache a bitmap file using the LruCache
     *
     * @param key Key used to identify the bitmap in the cache
     * @param bitmap The bitmap that is going to be cached
     * @param lruCache The cache memory where we are going to save the bitmap
     */
    public static void addBitmapToMemoryCache(String key, Bitmap bitmap, LruCache<String, Bitmap> lruCache) {
        if (getBitmapFromMemCache(key, lruCache) == null) {
            lruCache.put(key, bitmap);
        }
    }

    /**
     *
     * @param key Key used to identify the bitmap in the cache
     * @param lruCache The cache from where to retrieve the bitmap
     * @return The cached bitmap
     */
    public static Bitmap getBitmapFromMemCache(String key,LruCache<String, Bitmap> lruCache) {
        return lruCache.get(key);
    }

    /**
     *
     * @param context The context from which the method will be called
     * @param content The place where we are supposed to look for images
     * @return A list of images found in the specified content
     */
    public static List<ImageItem> getStoredImages(Context context, Uri content){
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        //Get images in the order that they were created
        final String orderBy = MediaStore.Images.Media._ID;
        //Stores all the images from the gallery in Cursor
        Cursor cursor = context.getContentResolver().query(
                content, columns, null,
                null, orderBy+" DESC");
        //If we have any records we parse them and return an ArrayList of images
        if (cursor != null) {
            int count = cursor.getCount();
            List<ImageItem> images = new ArrayList<>();
            if(count != 0)
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                //Store the path of the image
                File image = new File(cursor.getString(dataColumnIndex));
                //Add it to our ArrayList
                images.add(new ImageItem(image));
            }
            // The cursor should be freed up after use with close()
            cursor.close();
            return images;
        }

        //Create an array to store path to all the images
        return new ArrayList<>();
    }


    /**
     * Resize a bitmap for easier loading into memory
     *
     * @param filePath The path where the image can be found
     * @param reqWidth The width to be used for resizing
     * @param reqHeight The height to be used for resizing
     * @return The resized bitmap
     */
    public static Bitmap decodeSampledBitmapFromResource(String filePath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * Calculate the subsample parameter of the image
     * in order to load it faster and easier into memory
     *
     * @param options The original bitmap's height and width
     * @param reqWidth The width of the final bitmap
     * @param reqHeight The height of the final bitmap
     * @return The number of times an image should be subsampled
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
