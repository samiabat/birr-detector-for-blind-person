package com.example.imageclassifier;

import static android.system.Os.shutdown;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imageclassifier.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final int CAMERA_REQ_CODE = 100;

    private final int GALLERY_REQ_CODE = 101;

    String word;

    int imageSize = 224;
    Button btnCamera;
    Button imggallery;
    ImageView imgView;
    Bitmap bitmap;
    TextView result;
    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCamera = findViewById(R.id.btnCamera);
        imgView = findViewById(R.id.imgView);
        imggallery = findViewById(R.id.imggallery);
        result = findViewById(R.id.result);


        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v){
                //open an camera

                Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,CAMERA_REQ_CODE);
            }
        });

        imggallery.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v){
                //open an image

                Intent igallery = new Intent(Intent.ACTION_PICK);
                igallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(igallery,GALLERY_REQ_CODE);
            }
        });
    }


    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            float tot = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
                tot += confidences[i];
            }
            String[] classes = {"5 Birr", "10 Birr", "50 birr", "100 birr", "200 birr"};
            float confdent = (maxConfidence/tot) * 100;
            word = ("This is " + classes[maxPos] + " "+ confdent + " Confident Thank you");
            play(maxPos, confdent);
//            result.setText(word);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    int getMax(float [] arr){
        int loc = 0;
        float max = 0;
        for (int  i =0; i<arr.length; i++){
            if (arr[i] > max){
                max = arr[i];
                loc = i;
            }
        }
        return loc;
    }


    public void play(int loc, float confidet) {
        if (player == null) {
            if (confidet < 70){
                player = MediaPlayer.create(this, R.raw.rescan);
            }
            else if (loc == 0){
                player = MediaPlayer.create(this, R.raw.five);
            } else if (loc == 1){
                player = MediaPlayer.create(this, R.raw.ten);
            } else if (loc == 2){
                player = MediaPlayer.create(this, R.raw.fifty);
            } else if (loc == 3) {
                player = MediaPlayer.create(this, R.raw.hundred);
            } else {
                player = MediaPlayer.create(this, R.raw.two_hundred);
            }
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });

        }

        player.start();
    }

    public void stop(View v) {
        stopPlayer();
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "Thank you!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }




    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (result == RESULT_OK) {

            if (request == CAMERA_REQ_CODE) {

                //open an camera

                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
                imgView.setImageBitmap(bitmap);
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
                classifyImage(bitmap);

            } else if (request == GALLERY_REQ_CODE){
                // open gallery
                if (data != null){
                    Uri dat = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imgView.setImageBitmap(bitmap);
                    bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
                    classifyImage(bitmap);
                }
            }
        }
    }


    @Override
    public void onBackPressed() {

        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setIcon(R.drawable.photo2);
        alertDialogBuilder.setTitle(R.string.title);
        alertDialogBuilder.setMessage(R.string.massis);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface Dialog, int which){

                finish();

            }
        });


        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface Dialog, int which){

                Dialog.cancel();

            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}