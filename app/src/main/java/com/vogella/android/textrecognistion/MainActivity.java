package com.vogella.android.textrecognistion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button convertToSpeech;
    private Bitmap myBitmap;
    private ImageView myImageView;
    private EditText myEdittext;
    private TextToSpeech textToSpeech;


    public static final int WRITE_STORAGE = 100;
    public static final int SELECT_PHOTO = 102;
    public File photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "Welcome to TOO LAZY TO TYPE :)", Toast.LENGTH_SHORT).show();

        myEdittext = findViewById(R.id.edittext);
        myImageView = findViewById(R.id.imageView);
        convertToSpeech = findViewById(R.id.speak);
        findViewById(R.id.checkText).setOnClickListener(this);
        findViewById(R.id.select_image).setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.checkText:
                if (myBitmap != null) {
                    runTextRecognition();
                }
                break;
            case R.id.select_image:
                checkPermission(WRITE_STORAGE);
                break;
        }

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    //Select language
                    int lang = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        convertToSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textToConvert = myEdittext.getText().toString();
                int speech;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    speech = textToSpeech.speak(textToConvert,TextToSpeech.QUEUE_FLUSH,null,null);
                } else {
                    speech = textToSpeech.speak(textToConvert, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case WRITE_STORAGE:
                    checkPermission(requestCode);
                    break;
                case SELECT_PHOTO:
                    Uri dataUri = data.getData();
                    String path = CommonUtils.getPath(this, dataUri);
                    if (path == null) {
                        myBitmap = CommonUtils.resizePhoto(photo, this, dataUri, myImageView);
                    } else {
                        myBitmap = CommonUtils.resizePhoto(photo, path, myImageView);
                    }
                    if (myBitmap != null) {
                        myEdittext.setText(null);
                        myImageView.setImageBitmap(myBitmap);
                    }
                    break;

            }
        }
    }

    private void runTextRecognition() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText texts) {
                processExtractedText(texts);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure
                    (@NonNull Exception exception) {
                Toast.makeText(MainActivity.this,
                        "Exception", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processExtractedText(FirebaseVisionText firebaseVisionText) {
        myEdittext.setText(null);
        if (firebaseVisionText.getBlocks().size() == 0) {
            myEdittext.setText(R.string.no_text);
            return;
        }
        for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
            myEdittext.append(block.getText());

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_STORAGE:

                //If the permission request is granted, then...//
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //...call selectPicture//
                    selectPicture();
                    //If the permission request is denied, then...//
                } else {
                    //...display the “permission_request” string//
                    requestPermission(this, requestCode, R.string.permission_request);
                }
                break;

        }
    }

    //Display the permission request dialog//
    public static void requestPermission(final Activity activity, final int requestCode, int msg) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setMessage(msg);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Intent permissonIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                permissonIntent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(permissonIntent, requestCode);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alert.setCancelable(false);
        alert.show();
    }

    //Check whether the user has granted the WRITE_STORAGE permission//
    public void checkPermission(int requestCode) {
        switch (requestCode) {
            case WRITE_STORAGE:
                int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                //If we have access to external storage...//
                if (hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED) {
                //...call selectPicture, which launches an Activity where the user can select an image//
                    selectPicture();
                //If permission hasn’t been granted, then...//
                } else {
                //...request the permission//
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
                }
                break;

        }
    }

    private void selectPicture() {
        photo = CommonUtils.createTempFile(photo);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //Start an Activity where the user can choose an image//
        startActivityForResult(intent, SELECT_PHOTO);
    }


}