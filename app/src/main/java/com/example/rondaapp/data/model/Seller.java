package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Datos del vendedor tal como los devuelve el detalle de una publicación:
 * lo justo para decidir si conviene operar sin salir de la pantalla.
 */
public class Seller {

    private String id;
    private String name;
    private String zone;

    @SerializedName("created_at")
    private String createdAt;

    private Reputation reputation;

    /** Gson lo usa al deserializar la respuesta de la API. */
    public Seller() {}

    /** Para reconstruirlo desde el caché local (punto 6). */
    public Seller(String id, String name, String zone, String createdAt, Reputation reputation) {
        this.id = id;
        this.name = name;
        this.zone = zone;
        this.createdAt = createdAt;
        this.reputation = reputation;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getZone() { return zone; }

    /** Fecha de alta; de acá sale la antigüedad en la plataforma. */
    public String getCreatedAt() { return createdAt; }

    public Reputation getReputation() { return reputation; }
}
