package com.example.funfection;

public enum InfectionRates {
    LOW,
    MEDIUM,
    HIGH,
    OUTBREAK;

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
