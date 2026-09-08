package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SavedSearchResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<SavedSearch> data;

    @SerializedName("message")
    private String message;

    public SavedSearchResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<SavedSearch> getData() {
        return data;
    }

    public void setData(List<SavedSearch> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
