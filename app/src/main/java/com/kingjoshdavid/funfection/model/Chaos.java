package com.kingjoshdavid.funfection.model;

/**
 * Instability stat for a virus strain.
 *
 * <p>Chaos captures how volatile or unpredictable a strain is. Within the infection
 * system, it helps distinguish orderly, stable combinations from stranger offspring
 * that are more mutation-prone and narratively more dangerous.</p>
 *
 * <p>The dedicated wrapper makes it explicit when the code is dealing with mutation
 * pressure rather than basic spread or survival strength.</p>
 */
public final class Chaos extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Chaos(int score) {
        super(score);
    }

    /**
     * Creates a chaos score for a virus strain.
     *
     * @param score raw instability value
     * @return typed chaos wrapper for use in model and engine code
     */
    public static Chaos level(int score) {
        return new Chaos(score);
    }
}