package com.example.rondaapp;

import android.app.Application;
import android.util.Log;

import java.io.File;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Punto de arranque del contenedor de Hilt. La anotación es lo que genera el
 * componente que después inyecta en Activity y Fragments.
 */
@HiltAndroidApp
public class RondaApp extends Application {

    private static final String TAG = "RondaApp";

    /** Carpeta del caché offline anterior a Room, en JSON. */
    private static final String CACHE_JSON_VIEJO = "offline";

    @Override
    public void onCreate() {
        super.onCreate();
        borrarCacheJsonViejo();
    }

    /**
     * El caché offline se migró de archivos JSON a Room. Los archivos viejos ya
     * no los lee nadie, así que se borran una vez para no dejarlos ocupando
     * lugar en el dispositivo de quien venía usando la app.
     */
    private void borrarCacheJsonViejo() {
        File carpeta = new File(getFilesDir(), CACHE_JSON_VIEJO);
        if (!carpeta.exists()) return;

        new Thread(() -> {
            borrarRecursivo(carpeta);
            Log.i(TAG, "Caché JSON anterior a Room eliminado");
        }).start();
    }

    private void borrarRecursivo(File archivo) {
        File[] hijos = archivo.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) borrarRecursivo(hijo);
        }
        if (archivo.exists() && !archivo.delete()) {
            Log.w(TAG, "No se pudo borrar " + archivo);
        }
    }
}
