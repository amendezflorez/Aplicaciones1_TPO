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

    @SerializedName("photo_count")
    private int photoCount;

    /** Gson lo usa al deserializar la respuesta de la API. */
    public Publication() {}

    /** Para reconstruir la publicación desde el caché local (punto 6). */
    public Publication(int id, String title, String description, double price, String condition,
                       String category, String zone, String status, String createdAt,
                       String userId, String sellerName, int photoCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.condition = condition;
        this.category = category;
        this.zone = zone;
        this.status = status;
        this.createdAt = createdAt;
        this.userId = userId;
        this.sellerName = sellerName;
        this.photoCount = photoCount;
    }

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

    /** Cantidad de fotos. Los listados no traen los datos, solo el conteo. */
    public int getPhotoCount() { return photoCount; }
}
