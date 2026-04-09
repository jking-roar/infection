package com.kingjoshdavid.funfection.model;

/**
 * Display-oriented severity bands for a virus strain.
 *
 * <p>The game keeps the underlying stats numeric, but the UI needs short labels that are
 * easier to scan than raw totals. This enum is the bridge between those two views: it
 * maps the combined stat score into a readable outbreak tier.</p>
 *
 * <p>The thresholds are intentionally simple and fixed so that list summaries, detail
 * screens, and tests all report the same severity classification for the same strain.</p>
 */
public enum InfectionRates {
    /** Low combined threat and spread potential. */
    LOW,

    /** Noticeable infection risk, but still below major outbreak levels. */
    MEDIUM,

    /** Strong infection pressure that signals a dangerous strain. */
    HIGH,

    /** Extreme combined score associated with a major outbreak scenario. */
    OUTBREAK;

    /**
     * Converts a combined stat total into a display severity band.
     *
     * @param score combined infectivity, resilience, and chaos total
     * @return infection-rate tier used in summaries and detail views
     */
    public static InfectionRates fromScore(int score) {
        if (score <= 10) {
            return LOW;
        }
        if (score <= 18) {
            return MEDIUM;
        }
        if (score <= 24) {
            return HIGH;
        }
        return OUTBREAK;
    }
}
