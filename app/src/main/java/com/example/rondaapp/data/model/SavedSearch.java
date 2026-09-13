package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

public class SavedSearch {
    @SerializedName("id")
    private int id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("searchTerm")
    private String searchTerm;

    @SerializedName("category")
    private String category;

    @SerializedName("minPrice")
    private Double minPrice;

    @SerializedName("maxPrice")
    private Double maxPrice;

    @SerializedName("condition")
    private String condition;

    @SerializedName("zone")
    private String zone;

    @SerializedName("sort")
    private String sort;

    @SerializedName("createdAt")
    private String createdAt;

    public SavedSearch() {
    }

    public SavedSearch(String userId, String searchTerm) {
        this.userId = userId;
        this.searchTerm = searchTerm;
        this.sort = "recent";
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

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
