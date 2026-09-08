package com.example.rondaapp.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.rondaapp.data.model.Publication;

/**
 * Publicación guardada en el dispositivo para el modo sin conexión (punto 6).
 *
 * Una misma publicación puede estar en el último listado del Home, haber sido
 * abierta en detalle, o las dos cosas: por eso es una sola fila con dos marcas
 * en lugar de dos tablas que se pisarían.
 */
@Entity(tableName = "cached_publications")
public class CachedPublication {

    @PrimaryKey
    public int id;

    public String title;
    public String description;
    public double price;
    public String condition;
    public String category;
    public String zone;
    public String status;
    public String createdAt;
    public String userId;
    public String sellerName;
    public int photoCount;

    /** Vino en el último listado del Home que se cargó bien. */
    public boolean enListado;

    /** Cuándo se guardó ese listado; sirve para decir qué tan vieja es la info. */
    public long listadoGuardadoEn;

    /** Cuándo se abrió en detalle, o 0 si nunca. Ordena las "últimas vistas". */
    public long vistaEn;

    public CachedPublication() {}

    public static CachedPublication desde(@NonNull Publication p) {
        CachedPublication entidad = new CachedPublication();
        entidad.id = p.getId();
        entidad.title = p.getTitle();
        entidad.description = p.getDescription();
        entidad.price = p.getPrice();
        entidad.condition = p.getCondition();
        entidad.category = p.getCategory();
        entidad.zone = p.getZone();
        entidad.status = p.getStatus();
        entidad.createdAt = p.getCreatedAt();
        entidad.userId = p.getUserId();
        entidad.sellerName = p.getSellerName();
        entidad.photoCount = p.getPhotoCount();
        return entidad;
    }

    /** Vuelve al POJO que usan las vistas y Retrofit. */
    public Publication aPublication() {
        return new Publication(id, title, description, price, condition, category, zone,
                status, createdAt, userId, sellerName, photoCount);
    }
}
