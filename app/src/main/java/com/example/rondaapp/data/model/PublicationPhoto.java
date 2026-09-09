package com.example.rondaapp.data.model;

/**
 * Una foto de la galería. "data" es un data URI en base64: el backend no tiene
 * servidor de archivos, así que las fotos viajan dentro del JSON.
 */
public class PublicationPhoto {

    private int id;
    private String data;
    private int position;

    public int getId() { return id; }
    public String getData() { return data; }
    public int getPosition() { return position; }
}
