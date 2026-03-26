package com.example.ariellior.bigbrother;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface InstituteService {

    @POST("maps/addMarker")
    Call<Marker> addMarker(@Body Marker marker);
}
