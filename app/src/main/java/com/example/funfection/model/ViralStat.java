package com.example.funfection.model;

import java.io.Serializable;

/**
 * Base value object for the game's virus stat system.
 *
 * <p>The app models virus strength as three separate axes instead of one raw number:
 * infectivity for spread, resilience for survivability, and chaos for instability.
 * This shared base class keeps those wrappers lightweight while still giving each stat
 * a distinct type so engine code cannot accidentally swap one meaning for another.</p>
 *
 * <p>The score is intentionally stored as a plain integer because the rest of the game
 * logic combines, averages, and compares these values directly when producing offspring
 * viruses and outbreak ratings.</p>
 */
abstract class ViralStat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Raw numeric value for this stat dimension.
     */
    private final int score;

    /**
     * Creates a typed virus stat wrapper.
     *
     * @param score raw stat magnitude used by infection and display calculations
     */
    ViralStat(int score) {
        this.score = score;
    }

    /**
     * Returns the raw numeric score for this stat.
     *
     * @return integer value used by the infection engine and UI summaries
     */
    public int score() {
        return score;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ViralStat that = (ViralStat) other;
        return score == that.score;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode() + score;
    }

    @Override
    public String toString() {
        return Integer.toString(score);
    }
}