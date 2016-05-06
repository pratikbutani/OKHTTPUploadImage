package com.androidbuts.uploadimage;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidbuts.parser.JSONParser;
import com.androidbuts.permission.PermissionsActivity;
import com.androidbuts.permission.PermissionsChecker;
import com.androidbuts.utils.InternetConnection;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    /**
     * Permission List
     */
    private static final String[] PERMISSIONS_READ_STORAGE = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    /**
     * Context Variables
     */
    Context mContext;

    /**
     * Views
     */
    ImageView imageView;
    TextView textView;

    /**
     * Image path to send
     */
    String imagePath;

    /**
     *
     */
    PermissionsChecker checker;

    /**
     *
     */
    Toolbar toolbar;

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        /**
         * Permission Checker Initialized
         */
        checker = new PermissionsChecker(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checker.lacksPermissions(PERMISSIONS_READ_STORAGE)) {
                    startPermissionsActivity(PERMISSIONS_READ_STORAGE);
                } else {
                    showImagePopup();
                }
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(imagePath)) {

                    /**
                     * Uploading AsyncTask
                     */
                    if (InternetConnection.checkConnection(mContext)) {
                        new AsyncTask<Void, Integer, Boolean>() {

                            ProgressDialog progressDialog;

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                progressDialog = new ProgressDialog(MainActivity.this);
                                progressDialog.setMessage(getString(R.string.string_title_upload_progressbar_));
                                progressDialog.show();
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {

                                try {
                                    JSONObject jsonObject = JSONParser.uploadImage(imagePath);
                                    if (jsonObject != null)
                                        return jsonObject.getString("result").equals("success");

                                } catch (JSONException e) {
                                    Log.i("TAG", "Error : " + e.getLocalizedMessage());
                                }
                                return false;
                            }

                            @Override
                            protected void onPostExecute(Boolean aBoolean) {
                                super.onPostExecute(aBoolean);
                                if (progressDialog != null)
                                    progressDialog.dismiss();

                                if (aBoolean)
                                    Toast.makeText(getApplicationContext(), R.string.string_upload_success, Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(getApplicationContext(), R.string.string_upload_fail, Toast.LENGTH_LONG).show();

                                imagePath = "";
                                textView.setVisibility(View.VISIBLE);
                                imageView.setVisibility(View.INVISIBLE);
                            }
                        }.execute();
                    } else {
                        Snackbar.make(findViewById(R.id.parentView), R.string.string_internet_connection_warning, Snackbar.LENGTH_INDEFINITE).show();
                    }
                } else {
                    Snackbar.make(findViewById(R.id.parentView), R.string.string_message_to_attach_file, Snackbar.LENGTH_INDEFINITE).show();
                }
            }
        });
    }

    /**
     * Showing Image Picker
     */
    private void showImagePopup() {
        // File System.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_PICK);

        // Chooser of file system options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.string_choose_image));
        startActivityForResult(chooserIntent, 1010);
    }

    /***
     * OnResult of Image Picked
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1010) {
            if (data == null) {
                Snackbar.make(findViewById(R.id.parentView), R.string.string_unable_to_pick_image, Snackbar.LENGTH_INDEFINITE).show();
                return;
            }
            Uri selectedImageUri = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imagePath = cursor.getString(columnIndex);

                Picasso.with(mContext).load(new File(imagePath))
                        .into(imageView);
                cursor.close();

            } else {
                Snackbar.make(findViewById(R.id.parentView), R.string.string_unable_to_load_image, Snackbar.LENGTH_LONG).setAction("Try Again", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showImagePopup();
                    }
                }).show();
            }

            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startPermissionsActivity(String[] permission) {
        PermissionsActivity.startActivityForResult(this, 0, permission);
    }
}
