package com.kingjoshdavid.funfection.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;

import java.lang.reflect.Field;
import java.util.List;

final class RepositoryRoomTestSupport {

    private RepositoryRoomTestSupport() {
    }

    static FunfectionDatabase setUpInMemoryDatabase() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferencesUtil.resetForTesting();
        UserProfileRepository.resetForTesting();
        clearFallbackCollections();

        FunfectionDatabase database = Room.inMemoryDatabaseBuilder(context, FunfectionDatabase.class)
                .build();
        setDatabaseProvider(database);
        return database;
    }

    static void tearDownInMemoryDatabase(FunfectionDatabase database) throws Exception {
        setDatabaseProvider(null);
        if (database != null) {
            database.close();
        }
        clearFallbackCollections();
        UserProfileRepository.resetForTesting();
    }

    private static void clearFallbackCollections() throws Exception {
        clearStaticList(VirusRepository.class, "COLLECTION");
        clearStaticList(FriendsRepository.class, "FRIENDS");
    }

    @SuppressWarnings("unchecked")
    private static void clearStaticList(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        List<Object> value = (List<Object>) field.get(null);
        value.clear();
    }

    private static void setDatabaseProvider(FunfectionDatabase database) throws Exception {
        Field field = DatabaseProvider.class.getDeclaredField("database");
        field.setAccessible(true);
        field.set(null, database);
    }
}

