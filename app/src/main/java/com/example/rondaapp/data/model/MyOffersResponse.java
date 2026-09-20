package com.example.rondaapp.data.model;

import java.util.List;

/** Respuesta de GET offers/mine: lo que mandé y lo que recibí (punto 7). */
public class MyOffersResponse {

    private List<MyOffer> enviadas;
    private List<MyOffer> recibidas;

    public List<MyOffer> getEnviadas() { return enviadas; }
    public List<MyOffer> getRecibidas() { return recibidas; }
}
