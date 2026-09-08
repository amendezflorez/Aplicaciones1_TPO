package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FavoriteResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<Favorite> data;

    @SerializedName("message")
    private String message;

    public FavoriteResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Favorite> getData() {
        return data;
    }

    public void setData(List<Favorite> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
