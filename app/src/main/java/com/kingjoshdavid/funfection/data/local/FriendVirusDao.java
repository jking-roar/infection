package com.kingjoshdavid.funfection.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FriendVirusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FriendVirusCrossRef link);

    @Query("SELECT virusId FROM friend_virus_cross_ref WHERE friendId = :friendId ORDER BY linkedAt DESC")
    List<String> getVirusIdsByFriendId(String friendId);

    @Query("DELETE FROM friend_virus_cross_ref WHERE virusId = :virusId")
    int deleteByVirusId(String virusId);

    @Query("DELETE FROM friend_virus_cross_ref WHERE friendId = :friendId")
    int deleteByFriendId(String friendId);
}

