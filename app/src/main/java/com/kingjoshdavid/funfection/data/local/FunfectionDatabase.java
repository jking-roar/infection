package com.kingjoshdavid.funfection.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {VirusEntity.class, FriendEntity.class}, version = 1, exportSchema = false)
public abstract class FunfectionDatabase extends RoomDatabase {
    public abstract VirusDao virusDao();
    public abstract FriendDao friendDao();
}
