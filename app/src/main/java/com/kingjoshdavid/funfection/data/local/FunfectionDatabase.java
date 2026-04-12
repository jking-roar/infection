package com.kingjoshdavid.funfection.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {VirusEntity.class, FriendEntity.class, FriendUsernameHistoryEntity.class, FriendVirusCrossRef.class}, version = 7, exportSchema = false)
public abstract class FunfectionDatabase extends RoomDatabase {
    public abstract VirusDao virusDao();
    public abstract FriendDao friendDao();
    public abstract FriendUsernameHistoryDao friendUsernameHistoryDao();
    public abstract FriendVirusDao friendVirusDao();
}
