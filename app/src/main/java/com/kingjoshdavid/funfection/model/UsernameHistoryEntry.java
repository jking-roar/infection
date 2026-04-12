package com.kingjoshdavid.funfection.model;

public final class UsernameHistoryEntry {

    private final String username;
    private final long addedAt;

    public UsernameHistoryEntry(String username, long addedAt) {
        this.username = username == null ? "" : username.trim().replaceAll("\\s+", " ");
        this.addedAt = addedAt;
    }

    public String getUsername() {
        return username;
    }

    public long getAddedAt() {
        return addedAt;
    }
}
