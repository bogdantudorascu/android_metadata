package com.example.uni.photoristic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.uni.photoristic.Gallery.GalleryActivity;

/**
 * Activity used to display a single image fullscreen
 */
public class DisplayImageActivity extends AppCompatActivity {
    private ImageView imageView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);

        final CoordinatorLayout coordinatorLayout = findViewById(R.id.display_image_layout);

        Bundle b = getIntent().getExtras();
        imageView = (ImageView) findViewById(R.id.display_image);
        progressBar =  findViewById(R.id.loading_spinner);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_display_image);

        // check if extra data was passed from the calling activity
        if(b != null) {
            // this is the image position in the imagesItems list
            final int position = b.getInt("position");
            //When clicked the user will see the details of the image
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), ShowImageDetailsActivity.class);
                        intent.putExtra("position", position);
                        startActivity(intent);
                }
            });

            new UploadSingleImageTask().execute(MyImagesAdapter.getItems().get(position).imageFile.getAbsolutePath());
        } else Snackbar.make(coordinatorLayout,"Something went wrong",Snackbar.LENGTH_SHORT);

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

    /**
     * Asynchronously upload the image to the view
     */
    private class UploadSingleImageTask extends AsyncTask<String,Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... path) {

            Bitmap myBitmap = BitmapFactory.decodeFile(path[0]);

            return myBitmap;
        }

        @Override
        protected void onPostExecute (Bitmap myBitmap){
            imageView.setImageBitmap(myBitmap);
            progressBar.setVisibility(View.INVISIBLE);
        }

    }


}
