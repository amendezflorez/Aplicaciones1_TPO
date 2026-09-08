package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;
public class Publication {
    private int id;
    private String title;
    private String description;
    private double price;
    private String condition;
    private String category;
    private String zone;

    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("seller_name")
    private String sellerName;

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCondition() { return condition; }
    public String getCategory() { return category; }
    public String getZone() { return zone; }
    public String getCreatedAt() { return createdAt; }

    /** activa / pausada / vendida. */
    public String getStatus() { return status; }

    /** Id del vendedor: con esto se abre su perfil público. */
    public String getUserId() { return userId; }

    public String getSellerName() { return sellerName; }
}
