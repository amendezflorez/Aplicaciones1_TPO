package com.example.rondaapp.data.local;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.rondaapp.data.local.db.CacheDao;
import com.example.rondaapp.data.local.db.CachedPhoto;
import com.example.rondaapp.data.local.db.CachedPublication;
import com.example.rondaapp.data.model.Publication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto 6: caché en disco de lo último que se cargó bien del servidor.
 *
 * Respaldado por Room. Guarda dos cosas que conviven en la misma tabla: el
 * último listado del Home y las publicaciones que se abrieron en detalle con
 * sus fotos.
 *
 * Room prohíbe consultar en el hilo principal, así que todo corre en un
 * executor y las respuestas vuelven por callback al main thread, con la misma
 * forma que el enqueue de Retrofit que usa el resto de la app.
 */
public class OfflineCache {

    /** Tope de publicaciones vistas que se conservan, para no crecer sin límite. */
    private static final int MAX_VISTAS = 30;

    public interface Callback<T> {
        void onResultado(T resultado);
    }

    /**
     * Filtros del Home resueltos localmente. Son los mismos que se le mandan al
     * backend, para que sin conexión la lista se comporte igual.
     */
    public static class Filtros {
        public String busqueda;
        public String categoria;
        public String condicion;
        public Double minPrice;
        public Double maxPrice;
        /** Zonas a incluir: una para zona exacta, varias para cercanía. */
        public List<String> zonas;
        public String orden = "recent";
    }

    private final CacheDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public OfflineCache(CacheDao dao) {
        this.dao = dao;
    }

    // ---------- Listado del Home ----------

    /** Guarda la página que acaba de llegar bien del servidor. */
    public void guardarListado(List<Publication> publicaciones) {
        if (publicaciones == null || publicaciones.isEmpty()) return;

        List<CachedPublication> entidades = new ArrayList<>();
        for (Publication publicacion : publicaciones) {
            entidades.add(CachedPublication.desde(publicacion));
        }
        long ahora = System.currentTimeMillis();
        executor.execute(() -> dao.reemplazarListado(entidades, ahora));
    }

    /**
     * Lee el listado cacheado aplicando los mismos filtros y orden que se
     * usarían online. Con el caché en JSON esto no era posible y la lista
     * offline ignoraba lo que el usuario hubiera elegido.
     */
    public void leerListado(Filtros filtros, Callback<List<Publication>> callback) {
        executor.execute(() -> {
            Filtros f = (filtros != null) ? filtros : new Filtros();

            boolean sinBusqueda = f.busqueda == null || f.busqueda.trim().isEmpty();
            String patron = sinBusqueda ? "" : "%" + f.busqueda.trim() + "%";

            boolean sinZonas = f.zonas == null || f.zonas.isEmpty();
            // SQLite no acepta IN (): cuando el filtro no se usa se manda una
            // lista de relleno que el flag deja sin evaluar.
            List<String> zonas = sinZonas ? Collections.singletonList("") : f.zonas;

            List<CachedPublication> filas = dao.filtrar(
                    sinBusqueda ? 1 : 0, patron,
                    f.categoria, f.condicion,
                    sinZonas ? 1 : 0, zonas,
                    f.minPrice, f.maxPrice,
                    f.orden != null ? f.orden : "recent");

            List<Publication> resultado = new ArrayList<>();
            for (CachedPublication fila : filas) {
                resultado.add(fila.aPublication());
            }
            responder(callback, resultado);
        });
    }

    /** Momento en que se guardó el listado, o 0 si no hay nada cacheado. */
    public void guardadoEn(Callback<Long> callback) {
        executor.execute(() -> responder(callback, dao.guardadoEn()));
    }

    public void hayListado(Callback<Boolean> callback) {
        executor.execute(() -> responder(callback, dao.contarListado() > 0));
    }

    // ---------- Publicaciones vistas en detalle ----------

    /**
     * Guarda una publicación que la persona abrió, con sus fotos, para poder
     * volver a verla sin conexión.
     *
     * Pensado para que la pantalla de detalle (punto 4) lo llame apenas termina
     * de cargar.
     *
     * @param fotosBase64 data URI tal como los devuelve GET publications/{id}/photos
     */
    public void guardarVista(Publication publication, List<String> fotosBase64) {
        if (publication == null) return;

        CachedPublication entidad = CachedPublication.desde(publication);
        List<CachedPhoto> fotos = new ArrayList<>();
        if (fotosBase64 != null) {
            for (int i = 0; i < fotosBase64.size(); i++) {
                fotos.add(new CachedPhoto(publication.getId(), i, fotosBase64.get(i)));
            }
        }
        executor.execute(() -> dao.guardarVista(entidad, fotos, MAX_VISTAS));
    }

    public void leerVista(int publicationId, Callback<Publication> callback) {
        executor.execute(() -> {
            CachedPublication fila = dao.obtener(publicationId);
            responder(callback, (fila != null && fila.vistaEn > 0) ? fila.aPublication() : null);
        });
    }

    /** Fotos cacheadas de una publicación, como data URI, en orden. */
    public void leerFotos(int publicationId, Callback<List<String>> callback) {
        executor.execute(() -> {
            List<String> fotos = new ArrayList<>();
            for (CachedPhoto foto : dao.fotosDe(publicationId)) {
                fotos.add(foto.dataUri);
            }
            responder(callback, fotos);
        });
    }

    // ---------- Limpieza ----------

    /** Se llama al cerrar sesión: el caché es de la persona que estaba logueada. */
    public void limpiar() {
        executor.execute(dao::borrarTodo);
    }

    private <T> void responder(Callback<T> callback, T resultado) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResultado(resultado));
        }
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
