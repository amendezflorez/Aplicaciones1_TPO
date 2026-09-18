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
        // v2: CachedPublication guarda también el vendedor. Al subir la versión el
        // caché viejo se descarta (fallbackToDestructiveMigration) y se rearma solo.
        version = 2,
        exportSchema = false)
public abstract class RondaDatabase extends RoomDatabase {

    public abstract CacheDao cacheDao();
}
