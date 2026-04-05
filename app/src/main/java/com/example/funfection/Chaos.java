package com.example.funfection;

public final class Chaos extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Chaos(int score) {
        super(score);
    }

    public static Chaos level(int score) {
        return new Chaos(score);
    }
}