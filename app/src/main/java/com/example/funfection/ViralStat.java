package com.example.funfection;

import java.io.Serializable;

abstract class ViralStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int score;

    ViralStat(int score) {
        this.score = score;
    }

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