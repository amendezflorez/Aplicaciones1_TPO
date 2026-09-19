package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

public class RatingRequestDto {
    @SerializedName("estrellas")
    public int estrellas;
    
    @SerializedName("comentario")
    public String comentario;

    public RatingRequestDto(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
}
