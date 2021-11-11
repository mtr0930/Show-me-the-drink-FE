package com.example.Drinks_Classification;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.graphics.BlendMode.SRC_OVER;
import static android.graphics.PorterDuff.Mode.DST;
import static android.graphics.PorterDuff.Mode.DST_OVER;

public class CustomActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    PreviewView mCameraView;
    SurfaceHolder holder;
    SurfaceView surfaceView;
    ConstraintLayout rootLayout;
    ImageView imageView;
    Canvas canvas;
    String filePath;
    Paint paint;
    int cameraHeight, cameraWidth, xOffset, yOffset, boxWidth, boxHeight, screenWidth, screenHeight;
    Button btn_custom_camera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    int left, right, top, bottom, diameter;
    private static final int SET_IMAGE_VIEW_CODE = 555;


    /**
     * Starting Camera
     */
    public void startCamera(){
        mCameraView = findViewById(R.id.previewView);


        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = null;
            exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    public String getPathFromUri(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null );
        cursor.moveToNext();
        String path = cursor.getString( cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        cursor.close();
        return path;
    }
    /**
     *
     * Binding to camera
     */
    @SuppressLint("UnsafeExperimentalUsageError")
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();
        ImageCapture.Builder builder = new ImageCapture.Builder();
        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();


        preview.setSurfaceProvider(mCameraView.createSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);

        btn_custom_camera.setOnClickListener(v -> {

            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis());
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                    .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    .build();
            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CustomActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show();
                            Uri outputImage = outputFileResults.getSavedUri();
                            Bitmap imageBitmap = null;
                            Bitmap cropped_bitmap;
                            Intent intent = new Intent(CustomActivity.this, MainActivity.class);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                ImageDecoder.Source source_image = ImageDecoder.createSource(getContentResolver(), outputImage);

                                Log.d("성공", "여기까지 진행");
                                try {
                                    imageBitmap = ImageDecoder.decodeBitmap(source_image);

                                } catch (IOException e) {
                                    Log.d("실패", "여기는 에러");
                                    e.printStackTrace();
                                }
                                if(imageBitmap!=null){
                                    // camera width 1080
                                    // camera height 2015
                                    // box width 512
                                    // box height 1026
                                    // image width 3024
                                    // image height 4032
                                    // left 좌표 284, top 494
                                    int imageWidth = imageBitmap.getWidth();
                                    int imageHeight = imageBitmap.getHeight();

                                    String image_path = getPathFromUri(outputImage);
                                    int image_degree = readPictureDegree(image_path);
                                    float Bitmap_size = imageBitmap.getWidth() * imageBitmap.getHeight();
                                    Matrix rotationMatrix = new Matrix();
                                    rotationMatrix.postRotate(0);
                                    float x_scale = 1.0f;
                                    float y_scale = 1.0f;
                                    if(imageWidth > cameraWidth){
                                        x_scale = (float) (imageWidth / cameraWidth);
                                    }

                                    if(imageHeight > cameraHeight){
                                        y_scale = (float) (imageHeight / cameraHeight);
                                    }

//                                    int scaled_width = (int) (2.0 * boxWidth);
//                                    int scaled_height = (int) (2.0 * boxHeight);
                                    int scaled_width = (int) (x_scale * boxWidth);
                                    int scaled_height = (int) (y_scale * boxHeight);
                                    int x1 = (int) ((imageBitmap.getWidth() - scaled_width)/2) ;
                                    int y1 = (int)((imageBitmap.getHeight() - scaled_height)/2);
//                                    int width = (int)(2.0 * boxWidth);
//                                    int height = (int) (2.0 * boxHeight);
                                    int width = (int)(x_scale * boxWidth);
                                    int height = (int) (y_scale * boxHeight);
                                    Bitmap bitmap = Bitmap.createBitmap(imageBitmap, x1, y1, width, height, rotationMatrix, false);
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                                    Bitmap resize = Bitmap.createScaledBitmap(bitmap, 180, 360, true);
                                    resize.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                    byte[] byteArray = stream.toByteArray();

                                    intent.putExtra("img", byteArray);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }else{
                                    Log.d("실패", "image null");
                                }


                            }


                        }
                    });
                }
                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    error.printStackTrace();
                }
            });
        });


    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        //Start Camera
        startCamera();
        btn_custom_camera = findViewById(R.id.btn_custom_camera);
        imageView = findViewById(R.id.imageView);
        //Create the bounding box
        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(this);
        // Live detection and tracking
        ImageCapture imageCapture = new ImageCapture.Builder().build();



    }
    public String getBatchDirectoryName() {

        String app_folder_path = "";
        app_folder_path = Environment.getExternalStorageDirectory().toString() + "/images";
        Log.d("디렉토리 이름", app_folder_path);
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {

        }
        return app_folder_path;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        filePath = image.getAbsolutePath();
        return image;
    }

    /**
     *
     * For drawing the rectangular box
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void DrawFocusRect(int color) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        // 2015
        int height = mCameraView.getHeight();
        Log.d("성공", Integer.toString(height));
        // 1080
        int width = mCameraView.getWidth();
        Log.d("성공", Integer.toString(width));

        cameraHeight = height;
        cameraWidth = width;



        diameter = width;
        if (height < width) {
            diameter = height;
        }

        int offset = (int) (0.05 * diameter);
        diameter -= offset;

        canvas = holder.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(5);

        left = width / 2 - diameter / 4;
        top = height / 2 - diameter / 2;
        right = width / 2 + diameter / 4;
        bottom = height / 2 + diameter / 2;

        xOffset = left;
        yOffset = top;
        boxHeight = bottom - top;
        boxWidth = right - left;

        //Changing the value of x in diameter/x will change the size of the box ; inversely proportionate to x
        canvas.drawRect(left, top, right, bottom, paint);

        RectF rect = new RectF(left, top, right, bottom);
        Path path = new Path();
        path.addRect(rect, Path.Direction.CW);
        path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        canvas.clipPath(path);
        canvas.drawARGB(125, 0, 0, 0);

        holder.unlockCanvasAndPost(canvas);
    }
    private Point getPointOfView(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Point(location[0], location[1]);
    }

    /**
     * Callback functions for the surface Holder
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DrawFocusRect(Color.parseColor("#ff0000"));

        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }




}
