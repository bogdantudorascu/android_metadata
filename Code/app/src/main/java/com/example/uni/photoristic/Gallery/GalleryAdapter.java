package com.example.uni.photoristic.Gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.uni.photoristic.DisplayImageActivity;
import com.example.uni.photoristic.Extra.Common;
import com.example.uni.photoristic.HolderPositionAndPath;
import com.example.uni.photoristic.ImageItem;
import com.example.uni.photoristic.R;

import java.util.List;

/**
 * Class representing the adapter used to represent gallery images
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder>  {
    /**
     * Debug Tag for use logging debug output to LogCat
     */
    private static final String TAG = GalleryAdapter.class.getSimpleName();

    /**
     * Images to be displayed inside the adapter
     */
    private static List<ImageItem> items;

    /**
     * The calling activity's context
     */
    private Context context;

    /**
     * The cache used to store bitmaps
     */
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * Constructor used to create the adapter
     * @param imageItems A list of images to be displayed
     */
    public GalleryAdapter(List<ImageItem> imageItems){
        items = imageItems;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item_layout,
                parent, false);
        GalleryAdapter.ViewHolder holder = new GalleryAdapter.ViewHolder(v);
        context = parent.getContext();
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the
        // current row on the RecyclerView
        if (holder != null && items.get(position)!= null) {
            Bitmap myBitmap = Common.getBitmapFromMemCache(String.valueOf(position),mMemoryCache);
            if(myBitmap != null) {
                Log.v(TAG,"Using cached bitmap");
                holder.imageView.setImageBitmap(myBitmap);
                holder.progressBar.setVisibility(View.INVISIBLE);
            } else {
                Log.v(TAG,"Decoding bitmap and storing it");
                new GalleryAdapter.UploadSingleImageTask().execute(new HolderPositionAndPath(position, holder));
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, DisplayImageActivity.class);
                    intent.putExtra("position", holder.getAdapterPosition());
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Asynchronously decode and add a bitmap to cache
     * Set the decoded bitmap to the view
     */
    private class UploadSingleImageTask extends AsyncTask<HolderPositionAndPath,Void, Bitmap> {

        GalleryAdapter.ViewHolder holder;

        @Override
        protected Bitmap doInBackground(HolderPositionAndPath... holderAndPositions) {
            holder = (ViewHolder) holderAndPositions[0].getHolder();
            ImageItem imageItem = items.get(holderAndPositions[0].getPosition());
            Bitmap myBitmap = Common.decodeSampledBitmapFromResource(imageItem.getImageFile().getAbsolutePath(), 150, 150);

            Common.addBitmapToMemoryCache(String.valueOf(holderAndPositions[0].getPosition()),myBitmap,mMemoryCache);

            return myBitmap;
        }

        @Override
        protected void onPostExecute (Bitmap myBitmap){
            holder.imageView.setImageBitmap(myBitmap);
            holder.progressBar.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Class used to represent the structure of items inside the adapter
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
        
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_item_gallery);
            progressBar = itemView.findViewById(R.id.image_item_gallery_loading_spinner);
        }
    }
}
