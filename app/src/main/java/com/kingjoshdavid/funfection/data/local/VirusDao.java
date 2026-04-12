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

    @Query("SELECT created_at FROM viruses WHERE id = :id LIMIT 1")
    Long findCreatedAtById(String id);

    @Query("SELECT * FROM viruses WHERE seed = :seed ORDER BY created_at DESC LIMIT 1")
    VirusEntity findBySeed(long seed);

    @Query("SELECT * FROM viruses WHERE primary_patient_zero_id = :friendId"
            + " OR secondary_patient_zero_id = :friendId"
            + " OR combined_left_carrier_id = :friendId"
            + " OR combined_right_carrier_id = :friendId"
            + " ORDER BY created_at DESC")
    List<VirusEntity> findByAssociatedFriendId(String friendId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(VirusEntity virus);

    @Query("DELETE FROM viruses WHERE id = :id")
    int deleteById(String id);

    @Query("SELECT COUNT(*) FROM viruses")
    int count();
}
