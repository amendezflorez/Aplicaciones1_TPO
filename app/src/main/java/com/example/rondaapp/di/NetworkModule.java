package com.example.rondaapp.di;

import com.example.rondaapp.data.network.ApiService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Reemplaza al viejo RetrofitClient: ahora Hilt sabe cómo construir Retrofit y
 * el ApiService, y los reparte como una única instancia para toda la app.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    // 10.0.2.2 apunta al localhost de la máquina host desde el emulador de Android Studio
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
