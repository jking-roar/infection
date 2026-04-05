package com.example.funfection.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VirusTest {

    @Test
    public void toShareCodeSanitizesReservedCharacters() {
        Virus virus = new Virus("virus-1", "Spark:Name", "Spark", "Carrier|One",
            Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-123", "Fixture");

        assertEquals("virus-1:Spark:4:5:6:1:GEN-123:Spark-Name:Carrier/One", virus.toShareCode());
    }

    @Test
    public void getSummaryLineIncludesMutationLabelAndRate() {
        Virus virus = new Virus("virus-2", "Spark Name", "Spark", "Carrier",
            Infectivity.rate(9), Resilience.of(8), Chaos.level(8), true, "GEN-999", "Fixture");

        String summary = virus.getSummaryLine();

        assertTrue(summary.contains("Mutated"));
        assertTrue(summary.contains("Rate OUTBREAK"));
    }

    @Test
    public void getInfectionRateDelegatesToScoreBands() {
        Virus virus = new Virus("virus-3", "Stable", "Spark", "Carrier",
            Infectivity.rate(4), Resilience.of(4), Chaos.level(4), false, "GEN-444", "Fixture");

        assertEquals(InfectionRates.MEDIUM, virus.getInfectionRate());
    }
}