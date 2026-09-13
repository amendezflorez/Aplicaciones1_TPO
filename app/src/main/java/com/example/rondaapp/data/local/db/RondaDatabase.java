package com.example.rondaapp.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Base local de la app. Hoy solo guarda el caché offline del punto 6.
 *
 * Es un caché, no la fuente de verdad: si el esquema cambia se puede tirar y
 * reconstruir con la próxima carga del servidor, por eso el módulo de Hilt la
 * crea con fallbackToDestructiveMigration.
 */
@Database(
        entities = {CachedPublication.class, CachedPhoto.class},
        version = 1,
        exportSchema = false)
public abstract class RondaDatabase extends RoomDatabase {

    public abstract CacheDao cacheDao();
}
