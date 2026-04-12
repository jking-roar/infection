package com.kingjoshdavid.funfection.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FriendUsernameHistoryDao {

    @Query("SELECT * FROM friend_username_history WHERE friendId = :friendId ORDER BY added_at ASC")
    List<FriendUsernameHistoryEntity> getByFriendId(String friendId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FriendUsernameHistoryEntity entry);

    @Query("SELECT COUNT(*) FROM friend_username_history WHERE friendId = :friendId AND username = :username COLLATE NOCASE")
    int countByFriendIdAndUsername(String friendId, String username);

    @Query("DELETE FROM friend_username_history WHERE friendId = :friendId")
    int deleteByFriendId(String friendId);
}
