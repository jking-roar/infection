package com.kingjoshdavid.funfection.model;

/**
 * Transmission-strength stat for a virus strain.
 *
 * <p>Infectivity represents how easily a strain spreads between hosts. Higher values
 * make a virus feel more contagious in summaries and contribute directly to the
 * combined infection-rate band shown in the UI.</p>
 *
 * <p>This is a dedicated wrapper instead of an {@code int} so the engine can clearly
 * express when it is working with spread potential rather than durability or chaos.</p>
 */
public final class Infectivity extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Infectivity(int score) {
        super(score);
    }

    /**
     * Creates an infectivity score for a virus strain.
     *
     * @param score raw transmission strength value
     * @return typed infectivity wrapper for use in model and engine code
     */
    public static Infectivity rate(int score) {
        return new Infectivity(score);
    }
}