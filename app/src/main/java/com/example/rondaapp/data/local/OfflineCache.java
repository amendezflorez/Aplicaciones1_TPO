package com.example.rondaapp.data.local;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.rondaapp.data.model.Publication;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Punto 6: caché en disco de lo último que se cargó bien del servidor.
 *
 * Guarda dos cosas distintas:
 *  - el último listado del Home que llegó completo, para poder explorar sin
 *    conexión avisando que puede estar desactualizado;
 *  - las publicaciones que la persona abrió en detalle, con sus fotos, para
 *    poder volver a verlas offline.
 *
 * Es un caché de archivos con Gson, no una base: el proyecto no tiene Room ni
 * capa de persistencia, y meter una acá solo para esto sería desproporcionado.
 */
public class OfflineCache {

    private static final String TAG = "OfflineCache";
    private static final String CARPETA = "offline";
    private static final String ARCHIVO_LISTADO = "listado.json";
    private static final String ARCHIVO_VISTAS = "vistas.json";
    private static final String CARPETA_FOTOS = "fotos";

    /** Tope de publicaciones vistas que se conservan, para no crecer sin límite. */
    private static final int MAX_VISTAS = 30;

    private final Context context;
    private final Gson gson = new Gson();

    public OfflineCache(Context context) {
        this.context = context.getApplicationContext();
    }

    // ---------- Listado del Home ----------

    /** Envoltorio para poder guardar cuándo se cargó, no solo qué. */
    private static class ListadoCacheado {
        List<Publication> publicaciones;
        long guardadoEn;
    }

    public void guardarListado(List<Publication> publicaciones) {
        if (publicaciones == null || publicaciones.isEmpty()) return;

        ListadoCacheado cache = new ListadoCacheado();
        cache.publicaciones = publicaciones;
        cache.guardadoEn = System.currentTimeMillis();
        escribir(new File(carpeta(), ARCHIVO_LISTADO), gson.toJson(cache));
    }

    public List<Publication> leerListado() {
        ListadoCacheado cache = leerListadoCacheado();
        return (cache != null && cache.publicaciones != null)
                ? cache.publicaciones
                : Collections.emptyList();
    }

    /** Momento en que se guardó el listado, o 0 si no hay nada cacheado. */
    public long guardadoEn() {
        ListadoCacheado cache = leerListadoCacheado();
        return cache != null ? cache.guardadoEn : 0L;
    }

    public boolean hayListado() {
        return !leerListado().isEmpty();
    }

    private ListadoCacheado leerListadoCacheado() {
        String json = leer(new File(carpeta(), ARCHIVO_LISTADO));
        if (json == null) return null;
        try {
            return gson.fromJson(json, ListadoCacheado.class);
        } catch (RuntimeException e) {
            Log.w(TAG, "Caché de listado ilegible, se descarta: " + e.getMessage());
            return null;
        }
    }

    // ---------- Publicaciones vistas en detalle ----------

    /**
     * Guarda una publicación que la persona abrió, con sus fotos.
     *
     * Pensado para que la pantalla de detalle (punto 4) lo llame apenas termina
     * de cargar: con esto la misma publicación se puede volver a abrir sin
     * conexión.
     *
     * @param fotosBase64 data URI tal como los devuelve GET publications/{id}/photos
     */
    public void guardarVista(Publication publication, List<String> fotosBase64) {
        if (publication == null) return;

        Map<String, Publication> vistas = leerVistas();
        // Se reinserta para que quede al final: así el recorte saca las más viejas.
        vistas.remove(String.valueOf(publication.getId()));
        vistas.put(String.valueOf(publication.getId()), publication);

        while (vistas.size() > MAX_VISTAS) {
            String masVieja = vistas.keySet().iterator().next();
            vistas.remove(masVieja);
            borrarFotosDe(masVieja);
        }

        escribir(new File(carpeta(), ARCHIVO_VISTAS), gson.toJson(vistas));
        guardarFotos(publication.getId(), fotosBase64);
    }

    public Publication leerVista(int publicationId) {
        return leerVistas().get(String.valueOf(publicationId));
    }

    /** Fotos cacheadas de una publicación, como data URI, en orden. */
    public List<String> leerFotos(int publicationId) {
        File[] archivos = carpetaFotos().listFiles(
                (dir, nombre) -> nombre.startsWith(publicationId + "_"));
        if (archivos == null || archivos.length == 0) return Collections.emptyList();

        Arrays.sort(archivos, (a, b) -> a.getName().compareTo(b.getName()));
        List<String> fotos = new ArrayList<>();
        for (File archivo : archivos) {
            String contenido = leer(archivo);
            if (contenido != null) fotos.add(contenido);
        }
        return fotos;
    }

    private Map<String, Publication> leerVistas() {
        String json = leer(new File(carpeta(), ARCHIVO_VISTAS));
        if (json == null) return new LinkedHashMap<>();
        try {
            Map<String, Publication> vistas = gson.fromJson(
                    json, new TypeToken<LinkedHashMap<String, Publication>>() {}.getType());
            return vistas != null ? vistas : new LinkedHashMap<>();
        } catch (RuntimeException e) {
            Log.w(TAG, "Caché de vistas ilegible, se descarta: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private void guardarFotos(int publicationId, List<String> fotosBase64) {
        borrarFotosDe(String.valueOf(publicationId));
        if (fotosBase64 == null) return;

        for (int i = 0; i < fotosBase64.size(); i++) {
            // El índice va con dos dígitos para que el orden alfabético coincida
            // con el orden real de las fotos.
            String nombre = String.format("%d_%02d.txt", publicationId, i);
            escribir(new File(carpetaFotos(), nombre), fotosBase64.get(i));
        }
    }

    private void borrarFotosDe(String publicationId) {
        File[] archivos = carpetaFotos().listFiles(
                (dir, nombre) -> nombre.startsWith(publicationId + "_"));
        if (archivos == null) return;
        for (File archivo : archivos) {
            if (!archivo.delete()) Log.w(TAG, "No se pudo borrar " + archivo);
        }
    }

    // ---------- Archivos ----------

    private File carpeta() {
        File dir = new File(context.getFilesDir(), CARPETA);
        if (!dir.exists() && !dir.mkdirs()) Log.w(TAG, "No se pudo crear " + dir);
        return dir;
    }

    private File carpetaFotos() {
        File dir = new File(carpeta(), CARPETA_FOTOS);
        if (!dir.exists() && !dir.mkdirs()) Log.w(TAG, "No se pudo crear " + dir);
        return dir;
    }

    private void escribir(File archivo, String contenido) {
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            out.write(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "No se pudo escribir " + archivo.getName() + ": " + e.getMessage());
        }
    }

    private String leer(File archivo) {
        if (!archivo.exists()) return null;
        try {
            byte[] bytes = new byte[(int) archivo.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(archivo)) {
                int leidos = in.read(bytes);
                if (leidos <= 0) return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "No se pudo leer " + archivo.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /** Se llama al cerrar sesión: el caché es de la persona que estaba logueada. */
    public void limpiar() {
        borrarRecursivo(carpeta());
    }

    private void borrarRecursivo(File archivo) {
        File[] hijos = archivo.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) borrarRecursivo(hijo);
        }
        if (archivo.exists() && !archivo.delete()) Log.w(TAG, "No se pudo borrar " + archivo);
    }

    /** Utilidad para el detalle: pasar un data URI a bytes de imagen. */
    public static byte[] decodificarDataUri(String dataUri) {
        if (dataUri == null) return null;
        int coma = dataUri.indexOf(',');
        String base64 = (coma >= 0) ? dataUri.substring(coma + 1) : dataUri;
        try {
            return Base64.decode(base64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
