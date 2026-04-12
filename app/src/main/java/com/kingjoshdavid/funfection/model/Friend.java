package com.kingjoshdavid.funfection.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Friend {

    private final String id;
    private final String displayName;
    private final String displayNameOverride;
    private final String notes;
    private final String description;
    private final boolean protectedProfile;
    private final List<UsernameHistoryEntry> usernameHistory;
    private final long lastInfectionAt;

    public Friend(String id, String displayName) {
        this(id, displayName, "", "", "", false, Collections.emptyList(), 0L);
    }

    public Friend(String id,
                  String displayName,
                  String displayNameOverride,
                  String notes,
                  String description,
                  boolean protectedProfile,
                  List<UsernameHistoryEntry> usernameHistory) {
        this(id, displayName, displayNameOverride, notes, description, protectedProfile, usernameHistory, 0L);
    }

    public Friend(String id,
                  String displayName,
                  String displayNameOverride,
                  String notes,
                  String description,
                  boolean protectedProfile,
                  List<UsernameHistoryEntry> usernameHistory,
                  long lastInfectionAt) {
        this.id = id;
        this.displayName = normalize(displayName, "Unknown");
        this.displayNameOverride = normalize(displayNameOverride, "");
        this.notes = protectedProfile ? "" : normalize(notes, "");
        this.description = normalize(description, "");
        this.protectedProfile = protectedProfile;
        this.usernameHistory = Collections.unmodifiableList(normalizeHistory(usernameHistory));
        this.lastInfectionAt = Math.max(0L, lastInfectionAt);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }


    public String getDisplayNameOverride() {
        return displayNameOverride;
    }

    public String getNotes() {
        return notes;
    }

    public String getDescription() {
        return description;
    }

    public boolean isProtectedProfile() {
        return protectedProfile;
    }

    public String getEffectiveDisplayName() {
        return displayNameOverride.isEmpty() ? displayName : displayNameOverride;
    }

    public List<UsernameHistoryEntry> getUsernameHistory() {
        return usernameHistory;
    }

    public long getLastInfectionAt() {
        return lastInfectionAt;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static List<UsernameHistoryEntry> normalizeHistory(List<UsernameHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<UsernameHistoryEntry> normalized = new ArrayList<>();
        for (UsernameHistoryEntry entry : history) {
            if (entry == null) {
                continue;
            }
            String username = normalize(entry.getUsername(), "");
            if (username.isEmpty()) {
                continue;
            }
            boolean duplicate = false;
            for (UsernameHistoryEntry existing : normalized) {
                if (existing.getUsername().equalsIgnoreCase(username)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                normalized.add(new UsernameHistoryEntry(username, entry.getAddedAt()));
            }
        }
        return normalized;
    }
}
