package com.example.uni.photoristic;

import android.Manifest;
import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.uni.photoristic.Database.AppDatabase;
import com.example.uni.photoristic.Database.ImageData;
import com.example.uni.photoristic.Gallery.GalleryActivity;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShowImageDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    /**
     * Debug Tag for use logging debug output to LogCat
     */
    private static final String TAG = ShowImageDetailsActivity.class.getSimpleName();
    /**
     * The address of the server where the send photo details
     */
    private static final String SERVER_ADDRESS = "http://10.0.2.2:8091/uploadpicture";
    /**
     * Integer identifying requests in order to manage user actions
     */
    private static int REQUEST_PLACE_PICKER = 1;
    /**
     * Integer identifying requests in order to manage user actions
     */
    private static final int REQUEST_INTERNET = 2;

    //View fields
    EditText edit_image_description, edit_image_title, edit_image_time;
    String old_image_description, old_image_title, image_time;
    String new_image_description, new_image_title;
    Button change_location, update_data, send_data;

    /**
     * Constant used to store a reference to the database
     */
    private static AppDatabase db;

    /**
     * Variable used to store a reference to the current image item
     */
    private ImageItem imageItem;
    /**
     * Variable used to store a reference to the current image's location
     */
    private double[] imageLocation;
    /**
     * Variable used to store a reference to the map fragment
     */
    private SupportMapFragment mapFragment;
    /**
     * The current activity
     */
    private Activity context;
    /**
     * Layout containing the map
     */
    private LinearLayout map_layout;
    /**
     * The extension of the current image file
     */
    private String fileExtension;
    /**
     * The current image's location
     */
    private Marker photoMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image_details);
        edit_image_description = findViewById(R.id.edit_image_description);
        edit_image_title = findViewById(R.id.edit_image_title);
        edit_image_time = findViewById(R.id.edit_image_time);
        change_location = findViewById(R.id.change_location);
        update_data = findViewById(R.id.update_data);
        send_data = findViewById(R.id.send_data);

        context = this;


        map_layout = findViewById(R.id.map_layout);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_single_fragment);

        //Create or initialize the database if it is not already there
        if (db==null)
            db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "images_database")
                    .addMigrations(AppDatabase.MIGRATION_1_2)
                    .build();

        Bundle b = getIntent().getExtras();
        //Check to see if extra parameters have been passed form the calling activity
        if (b != null) {
            int position = b.getInt("position");
            //store the current image
            imageItem = MyImagesAdapter.getItems().get(position);
            //get the current's image extension
            fileExtension = imageItem.imageFile.getAbsolutePath().substring(imageItem.imageFile.getAbsolutePath().lastIndexOf("."));
            new GetImageDataFromDatabaseTask().execute();
        }

        // Modify the image's metadata
        update_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Only jpg/jpeg images can have metadata
                if(checkFileExtensionAndInformUser(fileExtension)) {
                    new_image_title = edit_image_title.getText().toString();
                    new_image_description = edit_image_description.getText().toString();
                    //if there are differences save them
                    if (!new_image_description.equals(old_image_description) || !new_image_title.equals(old_image_title)) {
                        new UpdateImageData().execute();
                    } else Toast.makeText(context, "There are no modifications",Toast.LENGTH_SHORT).show();
                }
            }});

        //Change(Add) the location of the current image
        change_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkFileExtensionAndInformUser(fileExtension)) {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    try {
                        startActivityForResult(builder.build(context), REQUEST_PLACE_PICKER);
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Google play services error. Please update/install",Toast.LENGTH_SHORT).show();
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Google play services error. Please update/install",Toast.LENGTH_SHORT).show();
                    }
                }
            }});

        //Send current's image data to the server
        send_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //First check if we need runtime permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //Check if we have internet permission
                    if (checkSelfPermission(Manifest.permission.INTERNET)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Internet Permission is granted" + Manifest.permission.INTERNET);
                        //Upload the image and its details
                        uploadImage(context,imageItem.imageFile,fileExtension, old_image_title, old_image_description,image_time,imageLocation);
                    } else {
                        Log.d(TAG, "Internet Permission is revoked");
                        ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET);
                    }
                } else {
                    uploadImage(context,imageItem.imageFile,fileExtension, old_image_title, old_image_description,image_time,imageLocation);
                }
            }});

        FloatingActionButton fab = findViewById(R.id.fab_update_data);
        //Refresh the shown details in case the user modified the photo outside our app
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryExifDataAndUpdateUI();
                Toast.makeText(context, "Details were refreshed",Toast.LENGTH_SHORT).show();
                new InsertIntoDatabaseTask().execute(imageItem.imageFile.getName(),old_image_title,old_image_description,image_time);
            }
        });

    }


    /**
     * Check if the extension of a file is .jpg or .jpeg
     *
     * @param extension The file path
     * @return A boolean representing whether the file is .jpg or .jpeg format
     */
    private boolean checkFileExtensionAndInformUser(String extension){
        if(!extension.equals(".jpg") && !extension.equals(".jpeg")){
            Toast.makeText(this,"Only jpg/jpeg file formats can have metadata",Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Manage user actions
        switch (requestCode) {
            case REQUEST_INTERNET:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Log.d(TAG,"Internet Permission: "+permissions[0]+ "was "+grantResults[0]);
                    uploadImage(context,imageItem.imageFile,fileExtension, old_image_title, old_image_description,image_time,imageLocation);
                } else {
                    // Permission Denied
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_INTERNET);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Method used to upload the image and its details to the server
     *
     * @param context The calling activity's context used to show information to the user
     * @param imageFile The image which we want to send
     * @param fileExtension The extension of the image
     * @param title The title of the image
     * @param description The description of the image
     * @param time The time/date of the image
     * @param location The location of the image
     */
    public static void uploadImage(final Activity context, File imageFile, String fileExtension, String title, String description, String time, double[] location) {

        final MediaType MEDIA_TYPE = MediaType.parse("image/"+fileExtension);

        MultipartBody.Builder reqBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image",imageFile.getName(),
                        RequestBody.create(MEDIA_TYPE,imageFile));
        if(title != null)
            reqBuilder.addFormDataPart("title", title);
        else reqBuilder.addFormDataPart("title", imageFile.getName());
        if(time != null)
            reqBuilder.addFormDataPart("date",time);
        if(description != null)
            reqBuilder.addFormDataPart("description",description);
        if(location != null) {
            reqBuilder.addFormDataPart("latitude", String.valueOf(location[0]));
            reqBuilder.addFormDataPart("longitude", String.valueOf(location[1]));
        }
        final RequestBody requestBody = reqBuilder.build();

        Request request = new Request.Builder()
                .url(SERVER_ADDRESS)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                //Use the UI thread to show information important to the user
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"Something went wrong with the request",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if(!response.isSuccessful())
                    //Use the UI thread to show information important to the user
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,"Something went wrong with the request",Toast.LENGTH_SHORT).show();
                        }
                    });
                else
                    //Use the UI thread to show information important to the user
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context,"Server response is: "+response.body().string(),Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(context,"Something went wrong with the response",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            }
        });
    }

    //manage user actions
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                ExifInterface exifInterface = null;
                try {
                    exifInterface = new ExifInterface(imageItem.imageFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(exifInterface != null) {
                    try {
                        LatLng newLocation = place.getLatLng();
                        exifInterface.setLatLong(newLocation.latitude,newLocation.longitude);
                        exifInterface.saveAttributes();
                        sendBroadcastToUpdateMediaFile();
                        Log.d(TAG,"Updating with: "+place.getName());
                        map_layout.setVisibility(View.VISIBLE);
                        imageLocation = new double[2];
                        imageLocation[0] = newLocation.latitude;
                        imageLocation[1] = newLocation.longitude;
                        // Get the SupportMapFragment and request notification
                        // when the map is ready to be used.

                        mapFragment.getMapAsync((OnMapReadyCallback) context);

                        new InsertIntoDatabaseTask().execute(imageItem.imageFile.getName(),old_image_title,old_image_description,image_time);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * Asynchronously update the image data
     */
    private class UpdateImageData extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //firstly start another async task to update the database
            new InsertIntoDatabaseTask().execute(imageItem.imageFile.getName(),new_image_title,new_image_description,image_time);
            //secondly update the metadata of the image
            ExifInterface exifInterface = null;
            try {
                exifInterface = new ExifInterface(imageItem.imageFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(exifInterface != null) {
                try {
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, new_image_title);
                    exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, new_image_description);
                    //Save the modifications
                    exifInterface.saveAttributes();
                    //Let the Media Scanner know that a specific file needs scanning
                    sendBroadcastToUpdateMediaFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void voids){
            Toast.makeText(context, "Image details were successfully updated",Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Asynchronously get image details from the database
     */
    private class GetImageDataFromDatabaseTask extends AsyncTask<Void,Void, List<ImageData>> {

        @Override
        protected List<ImageData> doInBackground(Void... params) {

            return db.imageDao().findImageByName(imageItem.imageFile.getName());
        }

        @Override
        protected void onPostExecute (List<ImageData> imageDataList){
            // Check to see if the image was found
            if(imageDataList.size() > 0){
                Log.d(TAG, "Using the database");
                ImageData imageData = imageDataList.get(0);
                String title = imageData.getTitle();
                String description = imageData.getDescription();
                String time = imageData.getTime();
                //update the UI
                updateUIImageDetails(title, description, time);
                if(imageData.getLatitude() != 0.0 && imageData.getLongitude() != 0.0){
                    change_location.setText(R.string.CHANGE_LOCATION);
                    map_layout.setVisibility(View.VISIBLE);
                    imageLocation = new double[2];
                    imageLocation[0] = imageData.getLatitude();
                    imageLocation[1] = imageData.getLongitude();
                    // Get the SupportMapFragment and request notification
                    // when the map is ready to be used.

                    mapFragment.getMapAsync((OnMapReadyCallback) context);
                }
                else {
                    change_location.setText(R.string.ADD_LOCATION);
                }

            } else {
                //There was no image found therefore we have to use the exif data
                queryExifDataAndUpdateUI();
                new InsertIntoDatabaseTask().execute(imageItem.imageFile.getName(),old_image_title,old_image_description,image_time);
            }
        }
    }

    private void queryExifDataAndUpdateUI(){
        Log.d(TAG, "Using the exif data");
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(imageItem.imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(exifInterface != null){
            old_image_title = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
            old_image_description = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT);
            image_time = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            updateUIImageDetails(old_image_title, old_image_description, image_time);
            imageLocation = exifInterface.getLatLong();
            if(imageLocation != null)
            Log.d(TAG,"Image location latitude is: "+imageLocation[0]);

            if(imageLocation != null){
                change_location.setText(R.string.CHANGE_LOCATION);
                map_layout.setVisibility(View.VISIBLE);
                mapFragment.getMapAsync((OnMapReadyCallback) context);
            } else {
                change_location.setText(R.string.ADD_LOCATION);
                if(photoMarker != null) {
                    photoMarker.remove();
                    map_layout.setVisibility(View.INVISIBLE);
                }
            }
        } else
            Toast.makeText(context, "Metadata could not be queried",Toast.LENGTH_SHORT).show();
    }

    /**
     * Asynchronously insert image details into the database
     */
    private class InsertIntoDatabaseTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            //if there is no location insert the data without
            if(imageLocation == null){
                ImageData imageData = new ImageData(params[0],params[1],params[2],params[3]);
                db.imageDao().insertImageData(imageData);
            }
            else {
                ImageData imageData = new ImageData(params[0],params[1],params[2],params[3],imageLocation[0], imageLocation[1]);
                db.imageDao().insertImageData(imageData);
            }
            return null;
        }

        @Override
        protected void onPostExecute (Void voids){
            Log.d(TAG, "Image inserted");
        }
    }

    /**
     * Method used to let the Media Scanner know that we have modified a file
     */
    private void sendBroadcastToUpdateMediaFile(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageItem.imageFile.getAbsolutePath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Method used to update fields on the UI
     * @param title Title of the image
     * @param description Description of the image
     * @param time Time of the image
     */
    private void updateUIImageDetails(String title, String description, String time){
        if(!TextUtils.isEmpty(title)) edit_image_title.setText(title);
        else {
            edit_image_title.setText(imageItem.imageFile.getName());
            old_image_title = imageItem.imageFile.getName();
        }
        if(!TextUtils.isEmpty(description))  edit_image_description.setText(description);
        else edit_image_description.setText(R.string.EXIF_UNKNOWN);
        if(!TextUtils.isEmpty(time))  edit_image_time.setText(time);
        else edit_image_time.setText(R.string.EXIF_UNKNOWN);
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        Log.d(TAG,"will set the marker");
        if(photoMarker!=null)
            photoMarker.remove();
        LatLng image_location = new LatLng(imageLocation[0], imageLocation[1]);
        //Add a marker on the map for the current image
        photoMarker = googleMap.addMarker(new MarkerOptions().position(image_location)
                .title(imageItem.imageFile.getName()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(image_location, 15));
    }
}
