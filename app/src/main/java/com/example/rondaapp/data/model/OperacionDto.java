package com.example.rondaapp.data.model;

import com.google.gson.annotations.SerializedName;

public class OperacionDto {
    @SerializedName("id")
    public int id;
    
    @SerializedName("articuloNombre")
    public String articuloNombre;
    
    @SerializedName("montoFinal")
    public double montoFinal;
    
    @SerializedName("contraparteNombre")
    public String contraparteNombre;
    
    @SerializedName("contraparteId")
    public String contraparteId;
    
    @SerializedName("fecha")
    public String fecha;
    
    @SerializedName("tipo")
    public String tipo;
    
    @SerializedName("fechaEntrega")
    public String fechaEntrega;
    
    @SerializedName("calificada")
    public boolean calificada;
}
