package com.example.funfection.data;

import com.example.funfection.model.UserProfile;

import java.util.Random;
import java.util.UUID;

/**
 * In-memory store for the local player's identity.
 */
public final class UserProfileRepository {

    private static final String[] LEFT_WORDS = {
            "Amber", "Brisk", "Clever", "Daring", "Frosty", "Lucky", "Mellow", "Nimble", "Quiet", "Solar"
    };

    private static final String[] RIGHT_WORDS = {
            "Badger", "Comet", "Falcon", "Lantern", "Otter", "Panda", "Sparrow", "Thunder", "Voyager", "Willow"
    };

    private static UserProfile currentUser;

    private UserProfileRepository() {
    }

    public static UserProfile getCurrentUser() {
        if (currentUser == null) {
            currentUser = new UserProfile(UUID.randomUUID().toString(), generateRandomUserName());
        }
        return currentUser;
    }

    public static UserProfile updateUserName(String userName) {
        UserProfile existing = getCurrentUser();
        String normalized = normalizeUserName(userName, existing.getUserName());
        currentUser = new UserProfile(existing.getId(), normalized);
        return currentUser;
    }

    public static void resetForTesting() {
        currentUser = null;
    }

    public static void setCurrentUserForTesting(UserProfile userProfile) {
        currentUser = userProfile;
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
}

