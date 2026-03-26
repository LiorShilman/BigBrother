package com.example.ariellior.bigbrother;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestService {
    private static final String URL = Constants.SIGNALR.REST_API_BASE;
    private Retrofit retrofit;
    private InstituteService apiService;

    public RestService() {
        retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(InstituteService.class);
    }

    public InstituteService getService() {
        return apiService;
    }
}
