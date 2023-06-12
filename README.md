## 시각장애인을 위한 캔음료수 분류 앱(Front-End)

### 1. Environment
 ```
📌 IDE : Android Studio (4.1.1)
   Language : Java
```
----



### 2. 주제 선정 이유
<img width="1391" alt="스크린샷 2023-06-12 오전 11 18 25" src="https://github.com/mtr0930/Drinks_Classification/assets/71540277/1df3e2a7-76f9-426a-9de3-26b95829c71a">

----



### 3. UI
<img width="1389" alt="스크린샷 2023-06-12 오후 2 17 26" src="https://github.com/mtr0930/Drinks_Classification/assets/71540277/8d41bc12-8184-4bd6-9be0-1174292ef3a8">

----



### 4. 아키텍쳐
<img width="1386" alt="스크린샷 2023-06-12 오후 2 18 15" src="https://github.com/mtr0930/Drinks_Classification/assets/71540277/c3d63262-b1d4-4ab3-b12f-1d063e0601b9">

----



### 5. 데모 영상
<https://youtu.be/OJpT2R3y27M>

----



### 6. 주요 기능들
#### TensorFlow Lite 모델 적용

> **백엔드 서버를 통해 `API서버`를 구현하고 요청을 보내서 응답을 받기에는 
이미지를 처리해야 되는 작업이기 때문에 오버헤드가 클 것이라고 예상했다.
`Android Studio` 와 연동도 되고 구현의 난이도도 고려해서 사용하게 됐다.**
> 
- **Tensor Flow Lite 모델 자세한 구현 Code**
    
    **TensorFlow Lite 모델을 지정된 Directory에 넣어주고 아래의 코드를 작성하면 
    모델의 적용이 완료됩니다.**
    
    ```java
    // 모델의 객체 선언
    FinalModel model = FinalModel.newInstance(getApplicationContext());
    
    // 모델의 입력 형식에 맞는 입력 객체 생성
    // 입력 형식 : 150 * 150 픽셀, BGR Color Image
    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);
    
    //converBitmapToByteBuffer를 통해서 bitmap정보를 정규화를 진행해서 byteBuffer에 넣어준다.
    ByteBuffer byteBuffer = **convertBitmapToByteBuffer**(img);
    inputFeature0.loadBuffer(byteBuffer);
    
    // 모델을 실행시키고 결과를 받음
    FinalModel.Outputs outputs = model.process(inputFeature0);
    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
    
    // 모델을 더이상 사용하지 않으므로 close함
    model.close();
    ```
    
    **위 Code에서 convertBitmapToByteBuffer 함수의 구현 설명
    이 함수를 통해 정규화를 진행해야 올바른 결과가 나옵니다.**
    
    ```java
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
            ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*150*150*3);
            imgData.order(ByteOrder.nativeOrder());
            //입력된 이미지를 원하는 사이즈의 bitmap으로 변환 filter: true를 통해 저화질 사진 보정가능
            Bitmap bitmap = Bitmap.createScaledBitmap(bp,150,150,true);
            int [] intValues = new int[150*150];
            //bitmap으로 부터 픽셀 정보를 가져와서 intValues에 넣어줌.
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    
            // 여기서 부터 정규화하는 부분
            int pixel = 0;
    
            for (int i = 0; i < 150; ++i) {
                for (int j = 0; j < 150; ++j) {
                    final int val = intValues[pixel++];
                    //0~255의 값은 8비트의 값 0xFF는 11111111을 의미한다.
                    //val의 값으로 들어오는 값은 R,G,B 세가지 필터에서 8개 비트씩 총 24개의 비트가 입력으로 들어온다.
                    //오른쪽으로 16번 shift하면 제일 앞에있던 8개 비트가 남게되는데 이를 0xFF와 and연산을 8비트의 결과로 나오게 해준다.
                    imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                    imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                    imgData.putFloat((val & 0xFF) / 255.f);
                }
            }
            return imgData;
        }
    ```
    
**Crop Box 이미지 변환 기능**
> **카메라의 레이아웃과 화면의 레이아웃 간의 차이가 있어서 
이를 변환하기 위해서 `width`와 `height`간의 비율을 적용시켰고  
캡쳐된 이미지가 깨지는 현상을 해결했다.**
> 

- **촬영버튼 클릭 이벤트 리스너 Code**
    
    ```java
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
                                        float global_scale = 1.0f;
                                        
                                        // width  비율 계산
                                        if(imageWidth > cameraWidth){
                                            x_scale = (float) (imageWidth / cameraWidth);
                                        }
                                        
                                        // height 비율 계산
                                        if(imageHeight > cameraHeight){
                                            y_scale = (float) (imageHeight / cameraHeight);
                                        }
    
                                        int scaled_width = (int) (x_scale * boxWidth);
                                        int scaled_height = (int) (y_scale * boxHeight);
                                        int x1 = (int) ((imageBitmap.getWidth() - scaled_width)/2) ;
                                        int y1 = (int)((imageBitmap.getHeight() - scaled_height)/2);

                                        int width = (int)(x_scale * boxWidth);
                                        int height = (int) (y_scale * boxHeight);
                                        Bitmap bitmap = Bitmap.createBitmap(imageBitmap, x1, y1, width, height, rotationMatrix, false);
                                        Uri original_uri = getImageUri(getApplicationContext(), bitmap);
    
                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
    
                                        Bitmap resize = Bitmap.createScaledBitmap(bitmap, 100, 200, true);
                                        
                                        resize.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                        byte[] byteArray = stream.toByteArray();
    
                                        intent.putExtra("img", byteArray);
                                        intent.putExtra("uri", original_uri);
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    }else{
                                        Log.d("실패", "image null");
                                    }
    
                                }
    
                            }
                        });
     }
    ```
    
#### `TTS`(Text to Speech) 기능

> **음료수를 알려줘는 시각 장애가 있는 분들을 위한 기능들을 제공하고 있다. 
큰 버튼 위주로 `UI`를 구성했고 음료수를 예측한 결과를 음성으로도 제공받을 수 있도록
결과 `Text`를 음성으로 출력하는 `TTS`를 사용했다.**
> 
- **`TTS`(Text to Speech) 자세한 구현 Code**
    
    ```java
    private void speak(){
    				// tv에 출력 결과가 나오는데 결과 메세지를 가져온다.
            String message = tv.getText().toString();
            Log.d("TTS", message);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
    						// 결과를 음성으로 출력한다.
                TTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            }
    }
    ```
    

