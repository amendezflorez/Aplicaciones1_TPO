package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/** Oferta de precio sobre una publicación (punto 4). */
public class Offer {

    private int id;
    private double amount;
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_name")
    private String userName;

    public int getId() { return id; }
    public double getAmount() { return amount; }

    /** pendiente por ahora; aceptar/rechazar queda para más adelante. */
    public String getStatus() { return status; }

    public String getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
}
