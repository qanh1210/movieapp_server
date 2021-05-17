package com.example.movieappserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.movieappserver.model.VideoUploadDetail;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Uri videoUri;
    TextView text_video_selected;
    String videoCategory;
    String videoTitle;
    String currentuid;
    StorageReference mstorageRef;
    StorageTask mUploadsTask;
    DatabaseReference referenceVideo;
    EditText video_description;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_video_selected = findViewById(R.id.txt_video_selected);
        video_description = findViewById(R.id.movies_description);
        referenceVideo = FirebaseDatabase.getInstance().getReference().child("videos");
        mstorageRef = FirebaseStorage.getInstance().getReference().child("videos");

        spinner = findViewById(R.id.spinner);

        List<String> categories = new ArrayList<String>();
        categories.add("Action");
        categories.add("Adventure");
        categories.add("Sports");
        categories.add("Romantics");
        categories.add("Comedy");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        videoCategory = adapterView.getItemAtPosition(position).toString();
        Toast.makeText(this, "selected"+videoCategory,Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void openVideoFiles(View view){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent,101);
    }


    private String getfileextension(Uri videouri) {

        ContentResolver contentResolver =getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return  mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(videouri));
    }

    private void uploadFiles(){
        if(videoUri != null){
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Video uploading...");
            progressDialog.show();
            StorageReference storageReference = mstorageRef.child(videoTitle + "."+getfileextension(videoUri));
            mUploadsTask = storageReference.putFile(videoUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uris) {
                            String video_url = uris.toString();
                            videoTitle = text_video_selected.getText().toString();
                            videoCategory = spinner.getSelectedItem().toString();
                            VideoUploadDetail videoUploadDetail = new VideoUploadDetail("","","",
                                    video_url,
                                    videoTitle,
                                    video_description.getText().toString(),
                                    videoCategory);
                            String uploadid = referenceVideo.push().getKey();
                            referenceVideo.child(uploadid).setValue(videoUploadDetail);
                            currentuid = uploadid;
                            progressDialog.dismiss();
                            if(currentuid.equals(uploadid)){
                                startThumbnailsActivity();
                            }
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred()/snapshot.getTotalByteCount());
                    progressDialog.setMessage("uploaded" + ((int )progress+"%...."));

                }
            });
        }
        else {
            Toast.makeText(this,"No video selected to upload", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 101 && resultCode == RESULT_OK && data.getData() != null){
            videoUri = data.getData();
            String fileNames = getFileName(videoUri);
            text_video_selected.setText(fileNames);
            String path = null;
            Cursor cursor;
            int column_index_data;
            String [] projection = {MediaStore.MediaColumns.DATA,MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media._ID, MediaStore.Video.Thumbnails.DATA};

            final String orderby = MediaStore.Video.Media.DEFAULT_SORT_ORDER;
            cursor = MainActivity.this.getContentResolver().query(videoUri,projection,null,null,orderby);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA) ;
            while(cursor.moveToNext()){
                path = cursor.getString(column_index_data);
                videoTitle = FilenameUtils.getBaseName(path);

            }
        }
    }

    private String getFileName(Uri uri) {

        String result = null;
        if(uri.getScheme().equals("content")){

            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
            finally {
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

    public void uploadFileToFirebase(View view){
        if(text_video_selected.equals("no video selected")){
            Toast.makeText(this,"Please selected an video", Toast.LENGTH_SHORT).show();
        }
        else {
            if(mUploadsTask != null && mUploadsTask.isInProgress()){
                Toast.makeText(this,"Video uploads is all ready in progress...",Toast.LENGTH_SHORT).show();
            }
            else {
                uploadFiles();
            }
        }
    }

    public void startThumbnailsActivity(){
        Intent intent = new Intent(MainActivity.this,UploadThumbnailActivity.class);
        intent.putExtra("currentuid",currentuid);
        intent.putExtra("thumbnailsName",videoTitle);
        startActivity(intent);
        Toast.makeText(this,"Uploaded successfully",Toast.LENGTH_LONG).show();

    }



}