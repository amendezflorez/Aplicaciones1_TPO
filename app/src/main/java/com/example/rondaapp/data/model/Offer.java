package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/** Oferta de precio sobre una publicación (punto 7). */
public class Offer {

    public static final String PENDIENTE = "pendiente";
    public static final String ACEPTADA = "aceptada";
    public static final String RECHAZADA = "rechazada";
    public static final String VENCIDA = "vencida";
    public static final String CONTRAOFERTADA = "contraofertada";

    private int id;
    private double amount;
    private String status;
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_name")
    private String userName;

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }

    /** Mensaje opcional que dejó el comprador junto con la oferta. */
    public String getMessage() { return message; }

    public String getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
}
