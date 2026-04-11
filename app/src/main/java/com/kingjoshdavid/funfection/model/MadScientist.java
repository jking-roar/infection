package com.kingjoshdavid.funfection.model;

/**
 * Immutable value object representing a named mad-scientist persona used as a
 * simulated-friend stand-in during random infection seeding.
 */
public final class MadScientist {

    private final String title;
    private final String name;

    public MadScientist(String title, String name) {
        this.title = title == null ? "" : title;
        this.name = name == null ? "" : name;
    }

    /** Honorific or prefix, e.g. {@code "Professor"} or {@code "The"}. */
    public String getTitle() {
        return title;
    }

    /** Surname or epithet, e.g. {@code "Tesla"} or {@code "Gutter Man"}. */
    public String getName() {
        return name;
    }

    /**
     * Returns the full display name as {@code "<title> <name>"} with surrounding
     * whitespace trimmed.
     */
    public String getDisplayName() {
        return (title + " " + name).trim();
    }

    @Override
    @androidx.annotation.NonNull
    public String toString() {
        return getDisplayName();
    }
}

