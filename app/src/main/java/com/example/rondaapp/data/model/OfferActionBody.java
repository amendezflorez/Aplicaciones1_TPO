package com.example.rondaapp.data.model;

/**
 * Body de PATCH publications/{id}/offers/{offerId}: aceptar, rechazar o
 * contraofertar (punto 7). El monto solo se usa para contraofertar.
 */
public class OfferActionBody {

    public static final String ACEPTAR = "aceptar";
    public static final String RECHAZAR = "rechazar";
    public static final String CONTRAOFERTAR = "contraofertar";

    private final String action;
    private final Double amount;

    public OfferActionBody(String action, Double amount) {
        this.action = action;
        this.amount = amount;
    }
}
