package com.example.rondaapp.data.local.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OperacionDao {
    @Query("SELECT * FROM operaciones WHERE (tipo = :tipo OR :tipo = 'TODOS') AND fechaTimestamp BETWEEN :inicio AND :fin ORDER BY fechaTimestamp DESC")
    LiveData<List<OperacionEntity>> getFilteredOperaciones(String tipo, long inicio, long fin);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OperacionEntity> operaciones);
}
