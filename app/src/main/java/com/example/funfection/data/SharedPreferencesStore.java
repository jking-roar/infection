package com.example.funfection.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Android SharedPreferences-backed preference store.
 */
final class SharedPreferencesStore implements PreferencesStore {

    private static final String PREF_FILE = "funfection.preferences";

    private final SharedPreferences sharedPreferences;

    SharedPreferencesStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    @Override
    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }
}
