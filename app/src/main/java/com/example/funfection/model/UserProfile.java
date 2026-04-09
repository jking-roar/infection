package com.example.funfection.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable local player identity used for ownership and origin display.
 */
public final class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userName;

    public UserProfile(String id, String userName) {
        this.id = id == null ? "" : id;
        this.userName = userName == null ? "" : userName;
    }

    public String getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }
}

