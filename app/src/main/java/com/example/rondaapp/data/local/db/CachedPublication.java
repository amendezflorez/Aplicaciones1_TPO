package com.example.rondaapp.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.Reputation;
import com.example.rondaapp.data.model.Seller;

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

    // ---------- Vendedor, tal como se vio en el detalle ----------
    // El TP pide conservar los "datos del vendedor" de lo que se consultó. El
    // listado no los trae, así que solo se completan al abrir el detalle.

    /**
     * Si se guardaron datos del vendedor. Sin esta marca no habría forma de
     * distinguir "nunca se guardó" de "tiene cero calificaciones", y la pantalla
     * terminaría afirmando lo segundo sin saberlo.
     */
    public boolean tieneVendedor;

    public String sellerZone;
    public String sellerCreatedAt;
    public double sellerAverage;
    public int sellerTotalRatings;
    public int sellerSalesCount;
    public int sellerPurchasesCount;

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

    /** Guarda el vendedor que trajo el detalle. Con null no toca nada. */
    public void guardarVendedor(Seller vendedor) {
        if (vendedor == null) return;
        tieneVendedor = true;
        if (vendedor.getName() != null) sellerName = vendedor.getName();
        sellerZone = vendedor.getZone();
        sellerCreatedAt = vendedor.getCreatedAt();

        Reputation reputacion = vendedor.getReputation();
        sellerAverage = reputacion != null ? reputacion.getAverage() : 0;
        sellerTotalRatings = reputacion != null ? reputacion.getTotalRatings() : 0;
        sellerSalesCount = reputacion != null ? reputacion.getSalesCount() : 0;
        sellerPurchasesCount = reputacion != null ? reputacion.getPurchasesCount() : 0;
    }

    /**
     * Arrastra el vendedor de una fila anterior. Lo usa el listado del Home, que
     * reescribe la publicación con datos que no traen reputación: sin esto, cada
     * recarga del Home borraba lo que se había guardado al abrir el detalle.
     */
    public void copiarVendedorDe(CachedPublication anterior) {
        if (anterior == null || !anterior.tieneVendedor) return;
        tieneVendedor = true;
        sellerZone = anterior.sellerZone;
        sellerCreatedAt = anterior.sellerCreatedAt;
        sellerAverage = anterior.sellerAverage;
        sellerTotalRatings = anterior.sellerTotalRatings;
        sellerSalesCount = anterior.sellerSalesCount;
        sellerPurchasesCount = anterior.sellerPurchasesCount;
    }

    /** El vendedor guardado, o null si nunca se abrió el detalle con conexión. */
    public Seller aSeller() {
        if (!tieneVendedor) return null;
        return new Seller(userId, sellerName, sellerZone, sellerCreatedAt,
                new Reputation(sellerAverage, sellerTotalRatings, sellerSalesCount, sellerPurchasesCount));
    }
}
