package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Favorite {
    @SerializedName("id")
    private int id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("publicationId")
    private int publicationId;

    @SerializedName("publication")
    private Publication publication;

    @SerializedName("savedAt")
    private String savedAt;

    @SerializedName("savedPrice")
    private double savedPrice;

    public Favorite() {
    }

    public Favorite(String userId, int publicationId) {
        this.userId = userId;
        this.publicationId = publicationId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(int publicationId) {
        this.publicationId = publicationId;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt;
    }

    public double getSavedPrice() {
        return savedPrice;
    }

    public void setSavedPrice(double savedPrice) {
        this.savedPrice = savedPrice;
    }
}
