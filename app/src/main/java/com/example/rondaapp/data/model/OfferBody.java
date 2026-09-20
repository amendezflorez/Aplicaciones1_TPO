package com.example.rondaapp.data.model;

/** Body de POST publications/{id}/offers. El ofertante sale del token. */
public class OfferBody {

    private final double amount;
    private final String message;

    public OfferBody(double amount, String message) {
        this.amount = amount;
        this.message = message;
    }
}
