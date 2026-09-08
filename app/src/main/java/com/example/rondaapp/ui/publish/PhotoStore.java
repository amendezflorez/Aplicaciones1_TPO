package com.example.rondaapp.ui.publish;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Guarda las fotos del borrador en el almacenamiento interno de la app.
 *
 * Las URI que devuelve el selector de la galería solo son legibles mientras
 * viva el proceso, así que un borrador que las guardara quedaría con fotos
 * rotas justo en el caso que el TP pide cubrir (salir de la app y volver).
 * Por eso se copia la imagen ya reescalada a filesDir apenas se elige.
 */
final class PhotoStore {

    private static final String TAG = "PhotoStore";
    private static final String CARPETA = "draft_photos";

    /** Lado máximo en píxeles. Suficiente para mostrar y acotado para viajar en base64. */
    private static final int LADO_MAXIMO = 1024;
    private static final int CALIDAD_JPEG = 80;

    private PhotoStore() {}

    static File carpeta(Context context) {
        File dir = new File(context.getFilesDir(), CARPETA);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "No se pudo crear " + dir);
        }
        return dir;
    }

    /**
     * Copia la imagen de la galería a un archivo propio, reescalada.
     * Devuelve null si no se pudo leer, para que la pantalla lo informe
     * en vez de guardar un borrador con una foto que no existe.
     */
    static File guardarDesdeUri(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;

            Bitmap original = BitmapFactory.decodeStream(in);
            if (original == null) return null;

            Bitmap reescalada = reescalar(original);
            File destino = new File(carpeta(context), UUID.randomUUID() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(destino)) {
                reescalada.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, out);
            }
            if (reescalada != original) reescalada.recycle();
            original.recycle();

            return destino;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "No se pudo guardar la foto: " + e.getMessage());
            return null;
        }
    }

    private static Bitmap reescalar(Bitmap original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int lado = Math.max(ancho, alto);
        if (lado <= LADO_MAXIMO) return original;

        float factor = (float) LADO_MAXIMO / lado;
        return Bitmap.createScaledBitmap(original,
                Math.round(ancho * factor), Math.round(alto * factor), true);
    }

    /** Convierte el archivo al data URI que espera el backend. */
    static String aDataUri(File archivo) {
        Bitmap bitmap = BitmapFactory.decodeFile(archivo.getAbsolutePath());
        if (bitmap == null) return null;

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        bitmap.recycle();

        String base64 = Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
        return "data:image/jpeg;base64," + base64;
    }

    /** Borra los archivos del borrador; se llama al publicar o al descartar. */
    static void limpiar(Context context) {
        File[] archivos = carpeta(context).listFiles();
        if (archivos == null) return;
        for (File archivo : archivos) {
            if (!archivo.delete()) Log.w(TAG, "No se pudo borrar " + archivo);
        }
    }

    static void borrar(File archivo) {
        if (archivo != null && archivo.exists() && !archivo.delete()) {
            Log.w(TAG, "No se pudo borrar " + archivo);
        }
    }
}
