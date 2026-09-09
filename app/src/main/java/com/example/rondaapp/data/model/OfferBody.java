package com.example.rondaapp.data.model;

/** Body de POST publications/{id}/offers. El ofertante sale del token. */
public class OfferBody {

    private final double amount;

    public OfferBody(double amount) {
        this.amount = amount;
    }
}
