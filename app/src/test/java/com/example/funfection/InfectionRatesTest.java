package com.example.funfection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InfectionRatesTest {

    @Test
    public void fromScoreMapsLowBand() {
        assertEquals(InfectionRates.LOW, InfectionRates.fromScore(10));
    }

    @Test
    public void fromScoreMapsMediumBand() {
        assertEquals(InfectionRates.MEDIUM, InfectionRates.fromScore(11));
        assertEquals(InfectionRates.MEDIUM, InfectionRates.fromScore(18));
    }

    @Test
    public void fromScoreMapsHighBand() {
        assertEquals(InfectionRates.HIGH, InfectionRates.fromScore(19));
        assertEquals(InfectionRates.HIGH, InfectionRates.fromScore(24));
    }

    @Test
    public void fromScoreMapsOutbreakBand() {
        assertEquals(InfectionRates.OUTBREAK, InfectionRates.fromScore(25));
    }
}