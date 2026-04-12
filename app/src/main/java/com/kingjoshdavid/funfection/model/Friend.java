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
    private final List<String> handleHistory;

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
                  List<String> handleHistory) {
        this.id = id;
        this.displayName = normalize(displayName, "Unknown");
        this.inviteCode = normalize(inviteCode, "");
        this.displayNameOverride = normalize(displayNameOverride, "");
        this.notes = protectedProfile ? "" : normalize(notes, "");
        this.description = normalize(description, "");
        this.protectedProfile = protectedProfile;
        this.handleHistory = Collections.unmodifiableList(normalizeHistory(handleHistory));
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

    public List<String> getHandleHistory() {
        return handleHistory;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static List<String> normalizeHistory(List<String> handleHistory) {
        if (handleHistory == null || handleHistory.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String handle : handleHistory) {
            String normalizedHandle = normalize(handle, "");
            if (normalizedHandle.isEmpty()) {
                continue;
            }
            boolean duplicate = false;
            for (String existing : normalized) {
                if (existing.equalsIgnoreCase(normalizedHandle)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                normalized.add(normalizedHandle);
            }
        }
        return normalized;
    }
}
