package com.kingjoshdavid.funfection.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY CASE WHEN protectedProfile THEN 1 ELSE 0 END ASC, lastInfectionAt DESC, createdAt DESC")
    List<FriendEntity> getAll();

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    FriendEntity findById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FriendEntity friend);

    @Query("DELETE FROM friends WHERE id = :id")
    int deleteById(String id);
}
