package com.example.rondaapp.di;

import android.content.Context;

import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.data.network.AuthInterceptor;
import com.example.rondaapp.session.SessionManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
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

    /** Sin esto, una red caída deja el request colgado hasta el timeout por defecto. */
    private static final long TIMEOUT_SEGUNDOS = 30;

    @Provides
    @Singleton
    public AuthInterceptor provideAuthInterceptor(@ApplicationContext Context context) {
        return new AuthInterceptor(new SessionManager(context));
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
