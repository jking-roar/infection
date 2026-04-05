package com.example.funfection.model;

public final class Infectivity extends ViralStat {

    private static final long serialVersionUID = 1L;

    private Infectivity(int score) {
        super(score);
    }

    public static Infectivity rate(int score) {
        return new Infectivity(score);
    }
}