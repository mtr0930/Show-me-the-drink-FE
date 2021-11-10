package com.example.Drinks_Classification;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private static final int CAMERA_PERMISSION_CODE=100;
    private static final int STORAGE_PERMISSION_CODE=101;
    private static final int SET_IMAGE_VIEW_CODE = 555;
    private static final int SELECT_PICTURE = 200;
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    // Text를 음성으로 출력하기 위한 모듈 TTS
    TextToSpeech TTS;
    // Firebase Firestore Cloud와 연결

    private ImageView imgView;
    private Button select, predict, camera;
    private TextView tv;
    private Bitmap img;
    private float[] results = new float[10];
    private String answer = "";
    float max = 0;
    int max_index = 0;
    String filePath;
    File imageFile;
    Uri photoURI;
    String BASE_URL = "http://192.168.123.110:5000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TTS = new TextToSpeech(this, this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

        imgView = (ImageView) findViewById(R.id.imageView);
        select = (Button) findViewById(R.id.btn_select);
        predict = (Button) findViewById(R.id.btn_predict);
        tv = (TextView) findViewById(R.id.tv_result);
        camera = (Button) findViewById(R.id.btn_camera);


        // select 버튼 클릭시 동작.
        select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                imageChooser();
            }
        });
        // camera 버튼 클릭시 동작.
        camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), CustomActivity.class);
                startActivityForResult(intent, SET_IMAGE_VIEW_CODE);
            }
        });
        // predict 버튼 클릭시 동작.
        predict.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if(photoURI!=null){
                    uploadFile(photoURI);
                    speak();
                }

            }
        });




    }// Oncreate끝.





    // 권한 요청 함수
    public void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {permission}, requestCode);
        }
        else{
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }
    // select 버튼 클릭시 실행되는 함수
    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_PICTURE);
    }
    // 권한 요청이 허용되면 toast message 출력
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == STORAGE_PERMISSION_CODE){

            if(grantResults.length > 0 &&  grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }

    }
    // 접근 권한이 있고, 사진을 성공적으로 intent로 가져왔으면 실행되는 부분
    // 촬영하거나 갤러리에서 선택한 이미지를 화면에 출력하는 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                imgView.setImageURI(data.getData());

                Uri uri = data.getData();

                if (null != uri){
                    photoURI = uri;
                    imgView.setImageURI(uri);
                }

            }

            if(requestCode == SET_IMAGE_VIEW_CODE){
                Log.d("성공", "on activity result");
                byte[] byteArray = (byte[]) data.getExtras().get("img");
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imgView.setImageBitmap(bitmap);
            }
        }
    }
    // TTS 객체를 initialization하기 위한 함수
    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS){
            int result = TTS.setLanguage(Locale.KOREAN);
            TTS.setSpeechRate(1);
            TTS.setPitch(1);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.d("TTS", "Language not supported");
            }
            else {
                predict.setEnabled(true);
                speak();
            }
        }
        else{
            Log.d("TTS", "Initialization failed");
        }
    }
    // Text를 음성으로 출력하는 함수
    private void speak(){
        String message = tv.getText().toString();
        Log.d("TTS", message);
        TTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);

    }



    private void uploadFile(Uri fileUri){
        FileUtils fileutils = new FileUtils(MainActivity.this);
        File originalFile = new File(fileutils.getPath(fileUri));
        RequestBody filePart = RequestBody.create(originalFile, MediaType.parse(getContentResolver().getType(fileUri)));
        MultipartBody.Part file = MultipartBody.Part.createFormData("file", originalFile.getName(), filePart);

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        UserClient client = retrofit.create(UserClient.class);
        Call<drinks> call = client.uploadPhoto(file);
        call.enqueue(new Callback<drinks>() {
            @Override
            public void onResponse(Call<drinks> call, Response<drinks> response) {
                Toast.makeText(MainActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
                if(!response.isSuccessful()){
                    Log.d("prediction", "response failed");
                    return;
                }

                String drink_name= response.body().getName();
                String drink_type = response.body().getType();
                String drink_flavor = response.body().getFlavor();
                String drink_cautions = response.body().getCautions();
                tv.setText("음료수 이름 : " + drink_name + "\n" +
                                "음료수 종류 : " + drink_type + "\n" +
                                "음료수 맛 : " + drink_flavor + "\n" +
                                "주의사항 : " + drink_cautions + "\n"
                );
                System.out.println("성공!!!!");
                System.out.println(drink_name);
                System.out.println(drink_type);
                System.out.println(drink_flavor);
                System.out.println(drink_cautions);
                speak();
            }

            @Override
            public void onFailure(Call<drinks> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

}// Activity 끝
