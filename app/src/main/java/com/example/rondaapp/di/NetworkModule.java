package com.example.rondaapp.di;

import com.example.rondaapp.data.network.PublicationApiService;
import com.example.rondaapp.data.network.RetrofitClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public static Retrofit provideRetrofit() {
        // En un proyecto real, configuraríamos el Retrofit aquí.
        // Usamos el cliente existente para mantener consistencia.
        return RetrofitClient.getClient(); 
    }

    @Provides
    @Singleton
    public static PublicationApiService providePublicationApiService(Retrofit retrofit) {
        return retrofit.create(PublicationApiService.class);
    }
}
