package com.kingjoshdavid.funfection.data.local;

import android.content.Context;

import androidx.room.Room;

public final class DatabaseProvider {

    private static final Object LOCK = new Object();
    private static volatile FunfectionDatabase database;

    private DatabaseProvider() {
    }

    public static FunfectionDatabase get(Context context) {
        if (database != null) {
            return database;
        }
        synchronized (LOCK) {
            if (database == null) {
                database = Room.databaseBuilder(context.getApplicationContext(),
                                FunfectionDatabase.class,
                                "funfection.db")
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return database;
    }

    public static FunfectionDatabase getIfInitialized() {
        return database;
    }
}
