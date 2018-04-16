package com.example.uni.photoristic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.uni.photoristic.Extra.Common;
import com.example.uni.photoristic.Gallery.GalleryActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleMap.OnMarkerClickListener {
    /**
     * Debug Tag for use logging debug output to LogCat
     */
    private static final String TAG = MainActivity.class.getSimpleName();

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
     * List of image markers
     */
    private List<ImageItemMarker> imageItemMarkers = new ArrayList<>();
    /**
     * Recycler view layout style
     */
    private LinearLayoutManager horizontalLayoutManagaer;
    /**
     * Current activity
     */
    private Activity activity;
    /**
     * Integer identifying requests in order to manage user actions - read external storage
     */
    private final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    /**
     * Integer identifying requests in order to manage user actions - write external storage
     */
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    /**
     * Integer identifying requests in order to manage user actions - location
     */
    private final int REQUEST_ACCESS_FINE_LOCATION = 3;
    /**
     * Integer identifying requests in order to manage user actions - camera
     */
    private final int REQUEST_CAMERA = 4;
    /**
     * Integer identifying requests in order to manage user actions - photo taken
     */
    private final int REQUEST_TAKE_PHOTO = 5;
    /**
     * The photo taken by the user
     */
    private File currentUserPhoto;

    /**
     * The map to be used to keep the user informed
     */
    private GoogleMap googleMap;
    /**
     * Request for location updates
     */
    private LocationRequest mLocationRequest;
    /**
     * Type of location that we want to receive
     */
    private FusedLocationProviderClient mFusedLocationClient;
    /**
     * The current location of the user
     */
    private Marker mMarkerCurrentLocation;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(areGooglePlayServicesAvailable(this)) {
            activity = this;
            // Get the SupportMapFragment and request notification
            // when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);

            mapFragment.getMapAsync(this);

            mRecyclerView = findViewById(R.id.horizontal_recycler_view);
            mAdapter = new MyImagesAdapter(imageItems);

            horizontalLayoutManagaer
                    = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
            mRecyclerView.setLayoutManager(horizontalLayoutManagaer);
            mRecyclerView.setAdapter(mAdapter);

            //Keep updating the map with marker while the user scroll through photos
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    Log.v(TAG, "Scroll state called");
                    int firstVisiblePosition = horizontalLayoutManagaer.findFirstVisibleItemPosition();
                    int lastVisiblePosition = horizontalLayoutManagaer.findLastVisibleItemPosition();
                    updateMapMarker(firstVisiblePosition, lastVisiblePosition);
                }
            });

            //Check for runtime permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Reading Permission is granted" + Manifest.permission.READ_EXTERNAL_STORAGE);
                    getPhotos();
                } else {
                    Log.v(TAG, "Reading Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                }
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Writing Permission is granted");
                } else {
                    Log.v(TAG, "Writing Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            } else {
                getPhotos();
            }

            FloatingActionButton fab = findViewById(R.id.fab);
            //Let the user take a photo
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        if(mMarkerCurrentLocation != null ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (checkSelfPermission(Manifest.permission.CAMERA)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    Log.v(TAG, "Camera Permission is granted");
                                    takePhotoAndSaveIt();
                                } else {
                                    Log.v(TAG, "Camera Permission is revoked");
                                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                                }
                            } else {
                                takePhotoAndSaveIt();
                            }
                        } else {
                            Toast.makeText(activity, "We haven't detected a position. Please enable the location.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "No camera present", Toast.LENGTH_SHORT).show();
                    }
                }

            });
        }

    }

    /**
     * Check for google play services availability. Our app will crash without
     * @param activity the calling activity
     * @return boolean representing whether or not the google play services are available
     */
    public boolean areGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        //Check for success
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                int REQUEST_UPDATE_GOOGLE_PLAY_SERVICES = 6;
                googleApiAvailability.getErrorDialog(activity, status, REQUEST_UPDATE_GOOGLE_PLAY_SERVICES).show();
            }
            Toast.makeText(this,"Update google play services and restart the app",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Start the intent to take a photo and save it
     */
    private void takePhotoAndSaveIt(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            currentUserPhoto = Common.savePhotoToExternalStorage();
            if (currentUserPhoto != null) {
                Log.v(TAG, "Will take photo now"+currentUserPhoto.getAbsolutePath()+" "+getApplicationContext().getPackageName());
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                /*
      The identifier of the taken photo
     */
                Uri currentUserPhotoUri = FileProvider.getUriForFile(activity, getApplicationContext().getPackageName() + ".provider", currentUserPhoto);
                Log.v(TAG,"My photo is: "+ currentUserPhotoUri);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentUserPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * Method used to keep udpating the map with markers of the visible photos
     * @param firstVisibleItemPosition the first visible item in the view
     * @param lastVisibleItemPosition the last visible item in the view
     */
    private void updateMapMarker(int firstVisibleItemPosition, int lastVisibleItemPosition) {
        if(googleMap != null){
            if(imageItems.size() > 0) {
                for (int i = 0; i < imageItemMarkers.size(); i++) {
                    ImageItemMarker imageItemMarker = imageItemMarkers.get(i);
                    if (imageItemMarker.position < firstVisibleItemPosition || imageItemMarker.position > lastVisibleItemPosition) {
                        imageItemMarkers.remove(imageItemMarker);
                        imageItemMarker.marker.remove();
                    }
                }
                for (int i = firstVisibleItemPosition; i < lastVisibleItemPosition; i++) {
                    File currentFile = imageItems.get(i).imageFile;
                    ExifInterface exifInterface = null;
                    try {
                        exifInterface = new ExifInterface(currentFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (exifInterface != null) {
                        double[] imageLocation = exifInterface.getLatLong();
                        if (imageLocation != null) {
                            LatLng image_location = new LatLng(imageLocation[0], imageLocation[1]);
                            Marker photoMarker = googleMap.addMarker(new MarkerOptions().position(image_location)
                                    .title(currentFile.getName()));
                            photoMarker.setTag(i);
                            imageItemMarkers.add(new ImageItemMarker(photoMarker, i));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //resume the location requests
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //pause the location requests
        Log.v(TAG,"Location updates stopped");
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);
    }

    /**
     * Callback used to update the location once a new one is received
     */
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location mCurrentLocation = locationResult.getLastLocation();
            String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            Log.i("MAP", "new location "+ mCurrentLocation.toString());
            if (googleMap!=null){
                if(mMarkerCurrentLocation != null){
                    mMarkerCurrentLocation.remove();
                    Log.v(TAG,"Update location");
                }
                mMarkerCurrentLocation =  googleMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("Your location at: "+ mLastUpdateTime));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),15));
            }

        }
    };

    /**
     * Method used to check for permission and start location updates if already available
     */
    private void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Location Permission is granted");
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback, null /* Looper */);
            } else {
                Log.v(TAG, "Location Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            Log.v(TAG, "Location permission is already enabled on a device lower than M");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, null /* Looper */);
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
            Toast.makeText(this,"You have no storage space mounted", Toast.LENGTH_SHORT);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "Got request code:"+requestCode);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    getPhotos();
                    Log.v(TAG,"Reading Permission: "+permissions[0]+ "was "+grantResults[0]);
                } else {
                    // Permission Denied
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                }
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Log.v(TAG,"Writing Permission: "+permissions[0]+ "was "+grantResults[0]);
                } else {
                    // Permission Denied
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;
            case REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG,"Location Permission: "+permissions[0]+ "was "+grantResults[0]);
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            mLocationCallback, null /* Looper */);
                } else {
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
                }
                break;
            case REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG,"Camera Permission: "+permissions[0]+ "was "+grantResults[0]);
                    takePhotoAndSaveIt();
                } else {
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(currentUserPhoto.getAbsolutePath());
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            imageItems.add(0,new ImageItem(currentUserPhoto));
            mAdapter.notifyDataSetChanged();
            updateMapMarker(0,0);
            Toast.makeText(activity,"You have successsfully taken a photo",Toast.LENGTH_SHORT).show();
//            Log.v(TAG,"Saved file image to gallery and id is"+ getIdFromURI(this, currentUserPhotoUri));
        }
    }

    /** Called when the map is ready. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMarkerClickListener(this);
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(Marker marker) {

        // Retrieve the data from the marker.
        Integer position = (Integer) marker.getTag();

        Log.v(TAG,"Marker clicked with position: "+position);

        if(position != null){
            Intent intent = new Intent(this, DisplayImageActivity.class);
            intent.putExtra("position", position);
            startActivity(intent);

            // Return true to indicate that we have consumed the event and that we wish
            // for the default behavior not to occur (which is for the camera to move such that the
            // marker is centered and for the marker's info window to open, if it has one).
            // We only want to start a new activity which contains the photo behind the marker
            return true;
        } else {
            // Return false to indicate that we have not consumed the event and that we wish
            // for the default behavior to occur (which is for the camera to move such that the
            // marker is centered and for the marker's info window to open, if it has one).
            return false;
        }
    }

    /**
     * Asynchronously get all external images and load them into the adapter
     */
    private class GetAllImagesAsync extends AsyncTask<Uri,Void,List<ImageItem>>{

        @Override
        protected List<ImageItem> doInBackground(Uri... uris) {
            return Common.getStoredImages(MainActivity.this, uris[0]);
        }

        @Override
        protected void onPostExecute (List<ImageItem> images){
            Log.d("ImageData", ""+images.size());
//            imageItems.add(images.get(0));
            if(images.size() > 0){
                imageItems.addAll(images);
                if(imageItems.size() > 0 && imageItems.size() > 5){
                    updateMapMarker(0,5);
                } else {
                    updateMapMarker(0,imageItems.size()%5);
                }
                mAdapter.notifyDataSetChanged();
            } else
                Toast.makeText(getApplicationContext(),"Unfortunately we found no images", Toast.LENGTH_SHORT);

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

        //noinspection SimplifiableIfStatement
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
