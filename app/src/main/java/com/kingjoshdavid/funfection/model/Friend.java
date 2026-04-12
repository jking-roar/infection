package com.kingjoshdavid.funfection.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Friend {

    private final String id;
    private final String displayName;
    private final String inviteCode;
    private final String displayNameOverride;
    private final String notes;
    private final String description;
    private final boolean protectedProfile;
    private final List<UsernameHistoryEntry> usernameHistory;

    public Friend(String id, String displayName, String inviteCode) {
        this(id, displayName, inviteCode, "", "", "", false, Collections.emptyList());
    }

    public Friend(String id,
                  String displayName,
                  String inviteCode,
                  String displayNameOverride,
                  String notes,
                  String description,
                  boolean protectedProfile,
                  List<UsernameHistoryEntry> usernameHistory) {
        this.id = id;
        this.displayName = normalize(displayName, "Unknown");
        this.inviteCode = normalize(inviteCode, "");
        this.displayNameOverride = normalize(displayNameOverride, "");
        this.notes = protectedProfile ? "" : normalize(notes, "");
        this.description = normalize(description, "");
        this.protectedProfile = protectedProfile;
        this.usernameHistory = Collections.unmodifiableList(normalizeHistory(usernameHistory));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInviteCode() {
        return inviteCode;
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

    public List<UsernameHistoryEntry> getUsernameHistory() {
        return usernameHistory;
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
