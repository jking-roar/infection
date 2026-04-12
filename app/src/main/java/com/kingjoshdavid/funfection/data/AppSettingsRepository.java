package com.kingjoshdavid.funfection.data;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores app-level display preferences (e.g. night mode) backed by shared preferences.
 */
public final class AppSettingsRepository {

    public enum NightMode {
        SYSTEM,
        LIGHT,
        NIGHT
    }

    private static final String PREF_NIGHT_MODE = "app_settings.night_mode";
    private static final String VALUE_LIGHT = "light";
    private static final String VALUE_NIGHT = "night";
    private static final String VALUE_SYSTEM = "system";

    private AppSettingsRepository() {
    }

    public static void initialize(Context context) {
        SharedPreferencesUtil.initialize(context);
    }

    public static NightMode getNightMode() {
        String stored = SharedPreferencesUtil.getString(PREF_NIGHT_MODE, VALUE_SYSTEM);
        if (VALUE_LIGHT.equals(stored)) return NightMode.LIGHT;
        if (VALUE_NIGHT.equals(stored)) return NightMode.NIGHT;
        return NightMode.SYSTEM;
    }

    public static void setNightMode(NightMode mode) {
        String value;
        switch (mode) {
            case LIGHT:
                value = VALUE_LIGHT;
                break;
            case NIGHT:
                value = VALUE_NIGHT;
                break;
            default:
                value = VALUE_SYSTEM;
                break;
        }
        SharedPreferencesUtil.putString(PREF_NIGHT_MODE, value);
    }

    /** Converts a {@link NightMode} to the corresponding {@link AppCompatDelegate} constant. */
    public static int toAppCompatNightMode(NightMode mode) {
        switch (mode) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case NIGHT:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /** Applies the stored night-mode preference to the entire app. */
    public static void applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(toAppCompatNightMode(getNightMode()));
    }
}
