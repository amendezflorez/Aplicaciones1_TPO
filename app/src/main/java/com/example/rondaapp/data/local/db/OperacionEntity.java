package com.example.rondaapp.data.local.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "operaciones")
public class OperacionEntity {
    @PrimaryKey
    public int id;
    
    public String articuloNombre;
    public double montoFinal;
    public String contraparteNombre;
    public long fechaTimestamp;
    public String tipo;
    public boolean calificada;
    public long fechaEntregaTimestamp;
}
