package com.example.funfection.data;

import android.content.Context;

import com.example.funfection.model.UserProfile;

import java.util.Random;
import java.util.UUID;

/**
 * Cached local player identity store backed by shared preferences.
 */
public final class UserProfileRepository {

    private static final String PREF_USER_ID = "user_profile.id";
    private static final String PREF_USER_NAME = "user_profile.name";

    private static final String[] LEFT_WORDS = {
            "Amber", "Brisk", "Clever", "Daring", "Frosty", "Lucky", "Mellow", "Nimble", "Quiet", "Solar"
    };

    private static final String[] RIGHT_WORDS = {
            "Badger", "Comet", "Falcon", "Lantern", "Otter", "Panda", "Sparrow", "Thunder", "Voyager", "Willow"
    };

    private static UserProfile currentUser;

    private UserProfileRepository() {
    }

    public static void initialize(Context context) {
        SharedPreferencesUtil.initialize(context);
    }

    public static UserProfile getCurrentUser() {
        if (currentUser == null) {
            String persistedId = SharedPreferencesUtil.getString(PREF_USER_ID, "");
            String persistedName = SharedPreferencesUtil.getString(PREF_USER_NAME, "");
            if (!persistedId.isEmpty() && !persistedName.isEmpty()) {
                currentUser = new UserProfile(persistedId, persistedName);
            } else {
                currentUser = new UserProfile(UUID.randomUUID().toString(), generateRandomUserName());
                persistCurrentUser();
            }
        }
        return currentUser;
    }

    public static UserProfile updateUserName(String userName) {
        UserProfile existing = getCurrentUser();
        String normalized = normalizeUserName(userName, existing.getUserName());
        currentUser = new UserProfile(existing.getId(), normalized);
        persistCurrentUser();
        return currentUser;
    }

    public static void resetForTesting() {
        currentUser = null;
        SharedPreferencesUtil.resetForTesting();
    }

    public static void setCurrentUserForTesting(UserProfile userProfile) {
        currentUser = userProfile;
        persistCurrentUser();
    }

    public static void clearCachedUserForTesting() {
        currentUser = null;
    }

    private static String generateRandomUserName() {
        Random random = new Random();
        return LEFT_WORDS[Math.abs(random.nextInt()) % LEFT_WORDS.length] + " "
                + RIGHT_WORDS[Math.abs(random.nextInt()) % RIGHT_WORDS.length];
    }

    private static String normalizeUserName(String candidate, String fallback) {
        String normalized = candidate == null ? "" : candidate.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    private static void persistCurrentUser() {
        if (currentUser == null) {
            SharedPreferencesUtil.remove(PREF_USER_ID);
            SharedPreferencesUtil.remove(PREF_USER_NAME);
            return;
        }
        SharedPreferencesUtil.putString(PREF_USER_ID, currentUser.getId());
        SharedPreferencesUtil.putString(PREF_USER_NAME, currentUser.getUserName());
    }
}
