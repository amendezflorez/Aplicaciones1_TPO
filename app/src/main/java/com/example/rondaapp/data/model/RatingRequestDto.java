package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

public class RatingRequestDto {
    @SerializedName("stars")
    public int estrellas;
    
    @SerializedName("comment")
    public String comentario;

    public RatingRequestDto(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
}
