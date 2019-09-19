package com.example.pentoscan.activities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.example.pentoscan.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.example.pentoscan.MainActivity;
import com.example.pentoscan.R;
import com.yalantis.ucrop.UCrop;


import java.io.File;

public class GalleryActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button btnGallery;
    private ProgressBar progressBar;
    private final int CODE_IMG_GALLERY = 1;
    private final String SAMPLED_CROPPED_IMG_NAME = "SampleCropImg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        init();
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent().setAction(Intent.ACTION_GET_CONTENT).setType("image/*"), CODE_IMG_GALLERY);
            }
        });

    }

    private void init() {
        this.btnGallery = findViewById(R.id.btnGalleryPic);
        this.imageView = findViewById(R.id.imageView);
        this.progressBar = findViewById(R.id.progressBar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_IMG_GALLERY && resultCode == RESULT_OK) {
//For Gallery
            Uri imageuri = data.getData();
            if (imageuri != null) {
                startCrop(imageuri);
            }

        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri imageUriResultCopy = UCrop.getOutput(data);
            if (imageUriResultCopy != null) {
//                imageView.setImageURI(imageUriResultCopy );
                try {

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUriResultCopy);
                    imageView.setImageBitmap(bitmap);
                    SharedPreferences mPrefs = getSharedPreferences("IDvalue", 0);
                    String str = mPrefs.getString("Token", "");

                    // Toast.makeText(this, str.toString(), Toast.LENGTH_SHORT).show();
                    uploadImage(bitmap, str);
                    progressBar.setVisibility(View.VISIBLE);


                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(GalleryActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    private void uploadImage(final Bitmap bitmap, final String token) {
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, API.OCR,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {

                        String resultResponse = null;
                        try {
                            resultResponse = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            resultResponse = resultResponse.replace("\"", "");
                            resultResponse = resultResponse.replace("\\n", " ");


                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(GalleryActivity.this, "You desired Result is Generated", Toast.LENGTH_LONG).show();
                        //Now Moving to text Editor
                        Intent intentTextEditor = new Intent(getApplicationContext(), MainActivity.class);
                        intentTextEditor.putExtra("textMessage", resultResponse);
                        startActivity(intentTextEditor);
                        finish();
                        //  iv_imageview.setImageBitmap(bitmap);

                        //            Log.e("VolleyOnResponse200", response.toString());
                        try {
                            JSONObject obj = new JSONObject(new String(resultResponse));
                            Log.i("response", obj + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VolleyonErrorResponse200", "Error: " + error.getMessage());
                        // Toasty.error(getApplicationContext(),"Something went wrong", Toast.LENGTH_SHORT).show();
                        NetworkResponse response = error.networkResponse;
                        if (error instanceof ServerError && response != null) {
                            try {
                                String res = new String(response.data,
                                        HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                                // Now you can use any deserializer to make sense of data
                                JSONObject obj = new JSONObject(res);
                            } catch (UnsupportedEncodingException e1) {
                                // Couldn't properly decode data to string
                                e1.printStackTrace();
                            } catch (JSONException e2) {
                                // returned data is not JSONObject?
                                e2.printStackTrace();
                            }

                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> header = new HashMap<String, String>();
                header.put("Authorization", "Token " + token);
                return header;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imagename = System.currentTimeMillis();
                params.put("pic", new DataPart(imagename + ".jpg", getFileDataFromDrawable(bitmap)));
                return params;
            }
        };
        volleyMultipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                999999999,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //adding the request to volley
        Volley.newRequestQueue(this).add(volleyMultipartRequest);

    }

    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = SAMPLED_CROPPED_IMG_NAME;
        destinationFileName += ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);
//       uCrop.withAspectRatio(3,4);
//        uCrop.useSourceImageAspectRatio();
//        uCrop.withAspectRatio(2,3);
//       uCrop.withAspectRatio(16,9 );
        uCrop.withMaxResultSize(450, 450);
        uCrop.withOptions(getCropOptions());
        uCrop.start(GalleryActivity.this);
    }

    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(70);
        //CompressType
//    options.setCompressionFormat(Bitmap.CompressFormat.PNG);
//    options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);
//UI
        options.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        options.setToolbarColor(getResources().getColor(R.color.colorPrimary));
//        options.setToolbarTitle("Image Cropper");
        return options;
    }


}
