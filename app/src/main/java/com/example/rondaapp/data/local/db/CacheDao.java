package com.example.rondaapp.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Acceso al caché offline (punto 6).
 *
 * Lo que justifica Room acá es {@link #filtrar}: con el caché en JSON el Home
 * sin conexión ignoraba búsqueda, filtros y orden y devolvía siempre la misma
 * lista. Acá es la misma consulta que hace el backend, contra lo guardado.
 */
@Dao
public interface CacheDao {

    // ---------- Listado del Home ----------

    /**
     * Mismos filtros que GET /api/publications, resueltos localmente.
     *
     * Los flags "sinX" existen porque SQLite no puede comparar contra una lista
     * vacía ni contra un LIKE nulo: cuando el filtro no se usa, la condición se
     * cortocircuita en 1 y el parámetro no se evalúa.
     *
     * @param zonas para "cercanía" se pasan los barrios linderos; para zona
     *              exacta, una lista de un solo elemento. Nunca vacía.
     */
    @Query("SELECT * FROM cached_publications WHERE enListado = 1 "
            + "AND (:sinBusqueda = 1 OR title LIKE :busqueda OR description LIKE :busqueda) "
            + "AND (:categoria IS NULL OR category = :categoria) "
            + "AND (:condicion IS NULL OR condition = :condicion) "
            + "AND (:sinZonas = 1 OR zone IN (:zonas)) "
            + "AND (:minPrice IS NULL OR price >= :minPrice) "
            + "AND (:maxPrice IS NULL OR price <= :maxPrice) "
            + "ORDER BY "
            + "  CASE WHEN :orden = 'price_asc'  THEN price  END ASC, "
            + "  CASE WHEN :orden = 'price_desc' THEN -price END ASC, "
            + "  createdAt DESC")
    List<CachedPublication> filtrar(int sinBusqueda, String busqueda,
                                    String categoria, String condicion,
                                    int sinZonas, List<String> zonas,
                                    Double minPrice, Double maxPrice,
                                    String orden);

    @Query("SELECT IFNULL(MAX(listadoGuardadoEn), 0) FROM cached_publications WHERE enListado = 1")
    long guardadoEn();

    @Query("SELECT COUNT(*) FROM cached_publications WHERE enListado = 1")
    int contarListado();

    /**
     * Reemplaza el listado cacheado conservando la marca de "vista": una
     * publicación que se abrió en detalle no debe perder sus fotos porque salió
     * de la primera página del Home.
     */
    @Transaction
    default void reemplazarListado(List<CachedPublication> nuevas, long guardadoEn) {
        desmarcarListado();
        for (CachedPublication publicacion : nuevas) {
            publicacion.enListado = true;
            publicacion.listadoGuardadoEn = guardadoEn;
            publicacion.vistaEn = vistaEnDe(publicacion.id);
        }
        insertar(nuevas);
        borrarNiListadasNiVistas();
    }

    @Query("UPDATE cached_publications SET enListado = 0")
    void desmarcarListado();

    @Query("SELECT IFNULL((SELECT vistaEn FROM cached_publications WHERE id = :id), 0)")
    long vistaEnDe(int id);

    /** Las que ya no están en el listado y tampoco se vieron no tienen por qué ocupar lugar. */
    @Query("DELETE FROM cached_publications WHERE enListado = 0 AND vistaEn = 0")
    void borrarNiListadasNiVistas();

    // ---------- Publicaciones vistas en detalle ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(List<CachedPublication> publicaciones);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(CachedPublication publicacion);

    @Query("SELECT * FROM cached_publications WHERE id = :id")
    CachedPublication obtener(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarFotos(List<CachedPhoto> fotos);

    @Query("SELECT * FROM cached_photos WHERE publicationId = :publicationId ORDER BY position")
    List<CachedPhoto> fotosDe(int publicationId);

    @Query("DELETE FROM cached_photos WHERE publicationId = :publicationId")
    void borrarFotosDe(int publicationId);

    /**
     * Guarda una publicación abierta en detalle, con sus fotos, y recorta las
     * vistas más viejas para que el caché no crezca sin límite.
     */
    @Transaction
    default void guardarVista(CachedPublication publicacion, List<CachedPhoto> fotos, int maxVistas) {
        CachedPublication existente = obtener(publicacion.id);
        if (existente != null) {
            // Si ya estaba en el listado, esa marca se conserva.
            publicacion.enListado = existente.enListado;
            publicacion.listadoGuardadoEn = existente.listadoGuardadoEn;
        }
        publicacion.vistaEn = System.currentTimeMillis();
        insertar(publicacion);

        borrarFotosDe(publicacion.id);
        if (fotos != null && !fotos.isEmpty()) {
            insertarFotos(fotos);
        }
        recortarVistas(maxVistas);
    }

    /** Borra la marca de vista de las más antiguas; las fotos se van por CASCADE. */
    @Query("DELETE FROM cached_photos WHERE publicationId IN ("
            + "  SELECT id FROM cached_publications WHERE vistaEn > 0 "
            + "  ORDER BY vistaEn DESC LIMIT -1 OFFSET :maxVistas)")
    void borrarFotosDeVistasViejas(int maxVistas);

    @Query("UPDATE cached_publications SET vistaEn = 0 WHERE id IN ("
            + "  SELECT id FROM cached_publications WHERE vistaEn > 0 "
            + "  ORDER BY vistaEn DESC LIMIT -1 OFFSET :maxVistas)")
    void desmarcarVistasViejas(int maxVistas);

    @Transaction
    default void recortarVistas(int maxVistas) {
        borrarFotosDeVistasViejas(maxVistas);
        desmarcarVistasViejas(maxVistas);
        borrarNiListadasNiVistas();
    }

    // ---------- Limpieza ----------

    @Query("DELETE FROM cached_publications")
    void borrarTodo();
}
