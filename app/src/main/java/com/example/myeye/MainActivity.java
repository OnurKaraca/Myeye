package com.example.myeye;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final String TAG = "MainActivity";
    private TextToSpeech textToSpeech;
    private Button captureButton;
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(this, this);

        captureButton = findViewById(R.id.button);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                } else {
                    dispatchTakePictureIntent();
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Upload the image to Flask server
            uploadImage(imageBitmap);
        }
    }

    private void uploadImage(Bitmap imageBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
                .build();

        Request request = new Request.Builder()
                .url("https://colt-musical-strictly.ngrok-free.app/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            private List<String> receivedTexts = new ArrayList<>();

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        String responseBodyString = response.body().string();
                        Log.i(TAG, "Gelen JSON: " + responseBodyString);


                        Log.i(TAG, "Metin verileri alındı");

                        try {
                            Log.i(TAG, "Gelen JSON: " + responseBodyString);
                            JSONObject jsonObject = new JSONObject(responseBodyString);

                            if (jsonObject.has("results")) {
                                String metinVerileriArray = jsonObject.getString("results");
                                textToSpeech.speak(metinVerileriArray, TextToSpeech.QUEUE_FLUSH, null, null);


                                Log.i(TAG, "Metin verileri alınmış");

                                speakReceivedTexts();
                            } else {
                                Log.e(TAG, "Sunucudan 'metin_verileri' anahtarı beklenen formatta gelmedi.");
                            }
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "JSON işleme hatası: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Image upload failed: " + response.code() + " " + response.message());
                    }
                }
                finally {
                    response.close();
                }
            }

            private void speakReceivedTexts() {
                // Metin verilerini TextToSpeech ile okuma işlemi yapabilirsiniz
                for (String metin : receivedTexts) {
                    Log.i(TAG, "Alınan metin: " + metin);
                    textToSpeech.speak(metin, TextToSpeech.QUEUE_FLUSH, null, null);
                }
                // Ardından, receivedTexts dizisini temizle, çünkü seslendirme tamamlandı
                receivedTexts.clear();
            }

            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    } else {
                        // Bu kısımda seslendirme işlemi yapılmayacak, çünkü onResponse içinde yapılacak
                    }
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                String textToRead = "HELLO!";
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }
}
