package com.example.qrapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class Wifi extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Wifi Class";
    // variable name changed .
    boolean mPermission = false;
    boolean isQRGenerated = false;

    Bitmap qrBits;
    String email;

    String encodedimage;
    String url = "https://qrphp.000webhostapp.com/upload.php";

    ProgressDialog pd;
    EditText editText_ssid, editText_pass;
    ImageView imageView, imageView_icon;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        editText_ssid = findViewById(R.id.ssid_input);
        editText_pass = findViewById(R.id.password_input);
        button = findViewById(R.id.creare_btn);
        imageView = findViewById(R.id.qrcode_image);
        imageView_icon = findViewById(R.id.show_pass_btn);
        SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
        email = preferences.getString("email", "null");

        // create qr code btn
        button.setOnClickListener(this);
        // hide show pass img
        imageView_icon.setOnClickListener(this);


    }

    // Action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mymenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.download:
                if (!isQRGenerated) {
                    Toast.makeText(this, "QR code not generated.!", Toast.LENGTH_SHORT).show();
                    break;
                }
                final Dialog d=new Dialog(Wifi.this);
                d.setContentView(R.layout.server_or_internal);
                Button server=d.findViewById(R.id.server);
                Button internal=d.findViewById(R.id.internal);
                d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                d.show();

                internal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // checkpermission() called and then if-else
                        // used to confirm for status of permission.
                        checkpermission();
                        if (!checkpermission()) {
                            checkpermission();
                        } else {
                            saveToGallery();
                        }
                        d.cancel();
                    }
                });
                server.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pd = new ProgressDialog(Wifi.this,R.style.MyAlertDialogStyle);
                        pd.setTitle("Connecting Server");
                        pd.setMessage("Storing at sever");
                        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        pd.show();
                        encodedbitmap(qrBits);
                        upload();
                        d.cancel();
                    }
                });
                break;

            case R.id.delete:
                deleteImage();
                break;

            case R.id.share:
                if (!isQRGenerated) {
                    Toast.makeText(this, "Please Create QR Code.!", Toast.LENGTH_SHORT).show();
                } else {
                    shareImage();
                }
                break;


            default:

        }
        return super.onOptionsItemSelected(item);

    }

    private void deleteImage() {
        if (!isQRGenerated) {
            Toast.makeText(this, "QR code not generated.!", Toast.LENGTH_SHORT).show();
        } else {
            imageView.setImageDrawable(null);
            editText_ssid.getText().clear();
            editText_pass.getText().clear();
            Toast.makeText(this, "Delete QR Code", Toast.LENGTH_SHORT).show();
            imageView.setBackgroundColor(Color.rgb(128, 128, 128));
            isQRGenerated = false;
        }
    }


    private void shareImage() {
        // share using File Provider

        Drawable drawable = imageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        try {
            File file = new File(getApplicationContext().getExternalCacheDir(), File.separator + "image.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);

            intent.putExtra(Intent.EXTRA_STREAM, photoURI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("image/png");

            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void saveToGallery() {
        // checking for imageview is empty or not.

        if (!isQRGenerated) {
            Toast.makeText(this, "QR code not generated.!", Toast.LENGTH_SHORT).show();
        } else {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();

            FileOutputStream fileOutputStream = null;
            File file = Environment.getExternalStorageDirectory();
            File dir = new File(file.getAbsolutePath() + "/Qr code");
            dir.mkdir();

            String filename = String.format("%d.png", System.currentTimeMillis());
            File outfile = new File(dir, filename);

            try {
                fileOutputStream = new FileOutputStream(outfile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                Toast.makeText(this, "QR code saved \nInternal storage/Qr code", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.d(TAG, "saveToGallery() EXCEPTION : " + e.getMessage());
                Toast.makeText(this, "Could not Download.!!!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    // permission for storage
    private boolean checkpermission() {
        // checkpermission returns boolean value.

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        Dexter.withActivity(this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        Log.d(TAG, "onPermissionsChecked() Report = " + report);
                        if (report.areAllPermissionsGranted()) {
                            mPermission = true;
                            Toast.makeText(Wifi.this, "Permissions granted", Toast.LENGTH_SHORT).show();
                        } else {
                            mPermission = false;
                            Toast.makeText(Wifi.this, "Permissions are Required.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }


                }).check();
        return mPermission;
    }

    // used to confirm if imageview is empty or not
    // BUT in this case its never empty as you have made its background grey  .
    private boolean hasImage(@NonNull ImageView view) {
        Drawable drawable = view.getDrawable();
        boolean hasImage = (drawable != null);

        if (hasImage && (drawable instanceof BitmapDrawable)) {
            hasImage = ((BitmapDrawable) drawable).getBitmap() != null;
        }

        return hasImage;
    }

    @Override
    public void onClick(View v) {
        //create qr code btn
        if (v == button) {
            String data = "ssid: " + (editText_ssid.getText().toString()) + "\n pass: " + (editText_pass.getText().toString());

            String data_ssid = editText_ssid.getText().toString();
            String data_pass = editText_pass.getText().toString();

            if (data_ssid.trim().isEmpty()) {
                editText_ssid.setError("Value Required.");
            } else if (data_pass.trim().isEmpty()) {
                editText_pass.setError("Value Required.");
            } else if (editText_pass.length() < 6) {
                editText_pass.setError("Please Enter Minimum 6 Char.");
            } else {
                QRGEncoder qrgEncoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, 300);

                try {
                    qrBits = qrgEncoder.getBitmap();

                    imageView.setImageBitmap(qrBits);

                    isQRGenerated = true;


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // hide show pass img
        if (v == imageView_icon) {
            if (v.getId() == R.id.show_pass_btn) {

                if (editText_pass.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                    imageView_icon.setImageResource(R.drawable.hide_password);

                    //Show Password
                    editText_pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    imageView_icon.setImageResource(R.drawable.show_password);

                    //Hide Password
                    editText_pass.setTransformationMethod(PasswordTransformationMethod.getInstance());

                }
            }

        }
    }
    private void upload() {


        final String name="Wifi:SSID-"+editText_ssid.getText().toString()+" Pass-"+editText_pass.getText().toString();

        StringRequest request=new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pd.dismiss();
                Toast.makeText(Wifi.this, ""+response.toString(), Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Wifi.this, "Please check your internet connection.Something went wrong.", Toast.LENGTH_SHORT).show();
            }
        }
        ){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> map=new HashMap<>();
                map.put("t1",name);
                map.put("upload",encodedimage);
                map.put("email",email);
                return map;
            }
        };

        RequestQueue queue= Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }



    private void encodedbitmap(Bitmap qrBits) {
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        qrBits.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        byte[] imgBytes=byteArrayOutputStream.toByteArray();
        encodedimage= Base64.encodeToString(imgBytes,Base64.DEFAULT);
    }
}

