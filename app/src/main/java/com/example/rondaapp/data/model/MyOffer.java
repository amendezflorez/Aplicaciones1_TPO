package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Punto 7: una fila de "Mis ofertas". Sirve tanto para las que mandé como
 * comprador como para las que recibí como vendedor; según la lista en la que
 * venga (ver MyOffersResponse), solo uno de los dos nombres de contraparte
 * trae dato.
 */
public class MyOffer {

    private int id;
    private double amount;
    private String status;
    private String message;

    @SerializedName("publication_id")
    private int publicationId;

    @SerializedName("publication_title")
    private String publicationTitle;

    @SerializedName("publication_price")
    private double publicationPrice;

    // Solo viene en "enviadas": quién vende la publicación.
    @SerializedName("seller_name")
    private String sellerName;

    // Solo viene en "recibidas": quién hizo la oferta.
    @SerializedName("buyer_name")
    private String buyerName;

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getPublicationId() { return publicationId; }
    public String getPublicationTitle() { return publicationTitle; }
    public double getPublicationPrice() { return publicationPrice; }
    public String getSellerName() { return sellerName; }
    public String getBuyerName() { return buyerName; }
}
