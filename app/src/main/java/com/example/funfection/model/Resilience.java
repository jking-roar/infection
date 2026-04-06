package com.example.funfection.model;

/**
 * Survivability stat for a virus strain.
 *
 * <p>Resilience represents how well a strain holds together once it has infected a host.
 * In the game's simplified model, this is the durability axis that balances raw spread
 * and contributes to the displayed infected-population and infection-rate summaries.</p>
 *
 * <p>Keeping resilience as its own type preserves intent throughout the codebase and
 * prevents mixing durability values with other stat categories by mistake.</p>
 */
public final class Resilience extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Resilience(int score) {
        super(score);
    }

    /**
     * Creates a resilience score for a virus strain.
     *
     * @param score raw durability value
     * @return typed resilience wrapper for use in model and engine code
     */
    public static Resilience of(int score) {
        return new Resilience(score);
    }
}