package com.example.rondaapp.data.model;

import java.util.List;

/**
 * Body de POST publications. Las fotos viajan como data URI en base64
 * (punto 5), por eso conviene no reutilizar este body para listados.
 *
 * No lleva el id del autor: el backend lo saca del token.
 */
public class CreatePublicationBody {

    private final String title;
    private final String description;
    private final double price;
    private final String condition;
    private final String category;
    private final String zone;
    private final List<String> photos;

    public CreatePublicationBody(String title, String description, double price,
                                 String condition, String category, String zone, List<String> photos) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.condition = condition;
        this.category = category;
        this.zone = zone;
        this.photos = photos;
    }
}
