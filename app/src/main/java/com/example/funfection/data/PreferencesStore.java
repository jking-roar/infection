package com.example.funfection.data;

/**
 * Minimal key-value preference contract used by repositories.
 */
public interface PreferencesStore {

    String getString(String key, String defaultValue);

    void putString(String key, String value);

    void remove(String key);
}
