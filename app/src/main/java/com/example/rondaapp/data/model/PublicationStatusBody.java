package com.example.rondaapp.data.model;

/**
 * Body de PATCH publications/{id}/status: activa / pausada / vendida.
 */
public class PublicationStatusBody {

    public static final String ACTIVA = "activa";
    public static final String PAUSADA = "pausada";
    public static final String VENDIDA = "vendida";

    private final String status;

    public PublicationStatusBody(String status) {
        this.status = status;
    }
}
