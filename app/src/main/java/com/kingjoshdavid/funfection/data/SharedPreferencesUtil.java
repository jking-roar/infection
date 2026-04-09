package com.kingjoshdavid.funfection.data;

import android.content.Context;

/**
 * Centralized utility for all repository preference access.
 */
final class SharedPreferencesUtil {

    private static PreferencesStore store = new InMemoryPreferencesStore();

    private SharedPreferencesUtil() {
    }

    static synchronized void initialize(Context context) {
        if (context == null) {
            return;
        }
        store = new SharedPreferencesStore(context.getApplicationContext());
    }

    static synchronized String getString(String key, String defaultValue) {
        return store.getString(key, defaultValue);
    }

    static synchronized void putString(String key, String value) {
        store.putString(key, value);
    }

    static synchronized void remove(String key) {
        store.remove(key);
    }

    static synchronized void resetForTesting() {
        store = new InMemoryPreferencesStore();
    }
}
