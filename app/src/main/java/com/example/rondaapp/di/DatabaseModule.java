package com.example.rondaapp.di;

import android.content.Context;

import androidx.room.Room;

import com.example.rondaapp.data.local.OfflineCache;
import com.example.rondaapp.data.local.db.CacheDao;
import com.example.rondaapp.data.local.db.RondaDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Persistencia local. Acompaña a {@link NetworkModule}, que ya repartía el
 * ApiService: con esto los fragments dejan de instanciar el caché a mano.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    private static final String NOMBRE_BASE = "ronda_cache.db";

    @Provides
    @Singleton
    public RondaDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, RondaDatabase.class, NOMBRE_BASE)
                // Es un caché: ante un cambio de esquema conviene descartarlo y
                // reconstruirlo con la próxima carga, no escribir migraciones.
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public CacheDao provideCacheDao(RondaDatabase database) {
        return database.cacheDao();
    }

    @Provides
    @Singleton
    public OfflineCache provideOfflineCache(CacheDao cacheDao) {
        return new OfflineCache(cacheDao);
    }
}
