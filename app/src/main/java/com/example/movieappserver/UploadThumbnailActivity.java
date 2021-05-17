package com.example.movieappserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.file.OpenOption;

public class UploadThumbnailActivity extends AppCompatActivity {

    Uri thumb_uri;
    String thumb_url;
    ImageView thumb_image;
    StorageReference storage_refThumb;
    DatabaseReference ref_Videos;
    TextView textSelected;
    RadioButton rad_btn_latest, rad_btn_popular, rad_btn_NoType, rad_btn_Slide;
    StorageTask storageTask;
    DatabaseReference update_data_ref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_thumbnail);

        textSelected = findViewById(R.id.txtViewthumSelected);
        thumb_image = findViewById(R.id.imageView);
        rad_btn_latest = findViewById(R.id.radiolatestmovies);
        rad_btn_popular = findViewById(R.id.radiopopularmovies);
        rad_btn_NoType = findViewById(R.id.radionnoselect);
        rad_btn_Slide = findViewById(R.id.radioSlidermovies);
        storage_refThumb = FirebaseStorage.getInstance().getReference().child("VideoThumbnails");
        ref_Videos = FirebaseDatabase.getInstance().getReference().child("vides");

        String currentUid = getIntent().getExtras().getString("currentuid");
        update_data_ref = FirebaseDatabase.getInstance().getReference("videos").child(currentUid);

        rad_btn_latest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String latestMovies = rad_btn_latest.getText().toString();
                update_data_ref.child("video_slide").setValue("");
                update_data_ref.child("video_type").setValue(latestMovies);
                Toast.makeText(UploadThumbnailActivity.this,"selected" +latestMovies,Toast.LENGTH_SHORT).show();

            }
        });

        rad_btn_NoType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String noTypeMovies = rad_btn_NoType.getText().toString();
                update_data_ref.child("video_type").setValue("");
                update_data_ref.child("video_slide").setValue("");
                Toast.makeText(UploadThumbnailActivity.this,"selected: No type",Toast.LENGTH_SHORT).show();

            }
        });

        rad_btn_popular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String popularMovies = rad_btn_popular.getText().toString();
                update_data_ref.child("video_slide").setValue("");
                update_data_ref.child("video_type").setValue(popularMovies);
                Toast.makeText(UploadThumbnailActivity.this,"selected" +popularMovies,Toast.LENGTH_SHORT).show();

            }
        });

        rad_btn_Slide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String slideMovies = rad_btn_Slide.getText().toString();
                update_data_ref.child("video_slide").setValue(slideMovies);
                Toast.makeText(UploadThumbnailActivity.this,"selected" +slideMovies,Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void showImageChooser(View view){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 102 && resultCode == RESULT_OK && data.getData() != null){
            thumb_uri = data.getData();
            try {
                String thumbnail = getFileName(thumb_uri);
                textSelected.setText(thumbnail);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),thumb_uri);
                thumb_image.setImageBitmap(bitmap);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private String getFileName(Uri uri){
        String result = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
            try{
                if(cursor != null && cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                }
            }finally {
                cursor.close();
            }
        }
        if(result == null){
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if(cut != -1){
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFile(){
        if(thumb_uri != null){
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Waiting for uploading thumbnail...");
            progressDialog.show();
            String video_title = getIntent().getExtras().getString("thumbnailsName");

          final StorageReference storageRef = storage_refThumb.child(video_title +"."+ getFileExtension(thumb_uri));

            storageRef.putFile(thumb_uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                             thumb_url = uri.toString();
                            String currentuid = getIntent().getExtras().getString("currentuid");
                            DatabaseReference updateData = FirebaseDatabase.getInstance()
                                    .getReference("videos")
                                    .child(currentuid);
                            updateData.child("video_thum").setValue(thumb_url);
                             progressDialog.dismiss();
                             Toast.makeText(UploadThumbnailActivity.this,"files uploaded",
                                     Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploaded" + ((int)progress)+"%...");
                }
            });

        }
    }

    public void uploadFileToFirebase(View view){
        if(textSelected.equals("no thumnail selected")){
            Toast.makeText(this,"first select an image",Toast.LENGTH_SHORT).show();
        }else {
            if(storageTask != null && storageTask.isInProgress()){
                Toast.makeText(this,"Upload files all ready in progress",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                uploadFile();
            }
        }
    }

    public String getFileExtension (Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
}