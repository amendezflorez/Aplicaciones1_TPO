package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/** Body enviado por el vendedor al aceptar una oferta, confirmando el punto de entrega. */
public class AcceptOfferBody {

    @SerializedName("delivery_point")
    private final String deliveryPoint;

    public AcceptOfferBody(String deliveryPoint) {
        this.deliveryPoint = deliveryPoint;
    }

    public String getDeliveryPoint() {
        return deliveryPoint;
    }
}
