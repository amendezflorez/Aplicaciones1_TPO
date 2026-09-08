package com.example.rondaapp.data.local.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Foto de una publicación cacheada, como data URI en base64 (el mismo formato
 * que devuelve el backend).
 *
 * La FK con borrado en cascada es lo que reemplaza al manejo a mano de archivos
 * sueltos: al sacar una publicación del caché, sus fotos se van solas.
 */
@Entity(
        tableName = "cached_photos",
        foreignKeys = @ForeignKey(
                entity = CachedPublication.class,
                parentColumns = "id",
                childColumns = "publicationId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("publicationId"))
public class CachedPhoto {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int publicationId;

    /** Orden en la galería, tal como vino del servidor. */
    public int position;

    public String dataUri;

    public CachedPhoto() {}

    public CachedPhoto(int publicationId, int position, String dataUri) {
        this.publicationId = publicationId;
        this.position = position;
        this.dataUri = dataUri;
    }
}
