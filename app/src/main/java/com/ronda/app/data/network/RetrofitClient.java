package com.ronda.app.data.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // 10.0.2.2 apunta al localhost de tu máquina desde el emulador de Android Studio.
    // Reemplazar por la URL real del backend cuando esté desplegado.
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    private static Retrofit instance;

    public static Retrofit getInstance() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }
}
