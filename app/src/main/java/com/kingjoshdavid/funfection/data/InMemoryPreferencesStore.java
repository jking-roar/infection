package com.kingjoshdavid.funfection.data;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory fallback preference store used when Android preferences are unavailable.
 */
final class InMemoryPreferencesStore implements PreferencesStore {

    private final Map<String, String> values = new HashMap<>();

    @Override
    public synchronized String getString(String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public synchronized void putString(String key, String value) {
        if (value == null) {
            values.remove(key);
            return;
        }
        values.put(key, value);
    }

    @Override
    public synchronized void remove(String key) {
        values.remove(key);
    }
}
