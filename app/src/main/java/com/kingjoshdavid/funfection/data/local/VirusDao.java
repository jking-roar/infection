package com.kingjoshdavid.funfection.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VirusDao {

    @Query("SELECT * FROM viruses ORDER BY created_at DESC")
    List<VirusEntity> getAll();

    @Query("SELECT * FROM viruses WHERE id = :id LIMIT 1")
    VirusEntity findById(String id);

    @Query("SELECT * FROM viruses WHERE seed = :seed ORDER BY created_at DESC LIMIT 1")
    VirusEntity findBySeed(long seed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(VirusEntity virus);

    @Query("DELETE FROM viruses WHERE id = :id")
    int deleteById(String id);

    @Query("SELECT COUNT(*) FROM viruses")
    int count();
}
