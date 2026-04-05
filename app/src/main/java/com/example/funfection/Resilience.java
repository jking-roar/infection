package com.example.funfection;

public final class Resilience extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Resilience(int score) {
        super(score);
    }

    public static Resilience of(int score) {
        return new Resilience(score);
    }
}