package com.kingjoshdavid.funfection.model;

public final class Friend {

    private final String id;
    private final String displayName;
    private final String inviteCode;

    public Friend(String id, String displayName, String inviteCode) {
        this.id = id;
        this.displayName = displayName;
        this.inviteCode = inviteCode;
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
}
