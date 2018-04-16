package com.example.uni.photoristic.Gallery;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.uni.photoristic.Extra.Common;
import com.example.uni.photoristic.ImageItem;
import com.example.uni.photoristic.MainActivity;
import com.example.uni.photoristic.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity used to display phone images
 */
public class GalleryActivity extends AppCompatActivity {
    /**
     * Debug Tag for use logging debug output to LogCat
     */
    private static final String TAG = GalleryActivity.class.getSimpleName();
    /**
     * Adapter used to add items and display them inside the recyclerView
     */
    private RecyclerView.Adapter  mAdapter;
    /**
     * RecyclerView used to display the images
     */
    private RecyclerView mRecyclerView;
    /**
     * List of images to be displayed
     */
    private List<ImageItem> imageItems = new ArrayList<>();
    /**
     * Current activity
     */
    private Activity activity;
    /**
     * The photo taken by the user
     */
    private File currentUserPhoto;
    /**
     * Integer identifying requests in order to manage user actions - read external storage
     */
    private final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        activity = this;
        // get the reference of RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.staggered_recycler_view);
        // set a StaggeredGridLayoutManager with vertical orientation and 3 columns
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(staggeredGridLayoutManager); // set LayoutManager to RecyclerView
        // initialize the adapter to be used with the recycler
        mAdapter = new GalleryAdapter(imageItems);
        mRecyclerView.setAdapter(mAdapter); // set the Adapter to RecyclerView

        // Check runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Reading Permission is granted" + Manifest.permission.READ_EXTERNAL_STORAGE);
                getPhotos();
            } else {
                Log.v(TAG, "Reading Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            getPhotos();
        }

    }


    /**
     * Method used to get all external photos of the phone
     */
    private void getPhotos(){
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(isSDPresent)
            new GetAllImagesAsync().execute(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        else
            Toast.makeText(this,"There is no external storage present", Toast.LENGTH_SHORT).show();
    }

    /**
     * Asynchronously get all the images and refresh the view
     */
    private class GetAllImagesAsync extends AsyncTask<Uri,Void,List<ImageItem>> {

        @Override
        protected List<ImageItem> doInBackground(Uri... uris) {
            return Common.getStoredImages(GalleryActivity.this, uris[0]);
        }

        @Override
        protected void onPostExecute (List<ImageItem> images){
            if(images.size() > 0){
                Log.d(TAG, ""+images.size());
                imageItems.addAll(images);
                mAdapter.notifyDataSetChanged();
            } else
                    Toast.makeText(getApplicationContext(),"Unfortunately we found no images", Toast.LENGTH_SHORT).show();

    }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.map_gallery) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        if (id == R.id.simple_gallery) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
