package com.example.rondaapp.data.model;

/**
 * Respuesta de GET publications/{id}: la publicación y su vendedor.
 * El vendedor puede venir null si la publicación quedó sin dueño.
 */
public class PublicationDetailResponse {

    private Publication publication;
    private Seller seller;

    public Publication getPublication() { return publication; }
    public Seller getSeller() { return seller; }
}
