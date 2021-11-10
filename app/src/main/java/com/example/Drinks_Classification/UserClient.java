package com.example.Drinks_Classification;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UserClient {
    @Multipart
    @POST("uploadfile")
    Call<drinks> uploadPhoto(@Part MultipartBody.Part photo);
}
