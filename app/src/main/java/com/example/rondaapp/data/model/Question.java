package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

/** Pregunta que dejó un interesado en una publicación (punto 4). */
public class Question {

    private int id;
    private String text;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_name")
    private String userName;

    public int getId() { return id; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
}
