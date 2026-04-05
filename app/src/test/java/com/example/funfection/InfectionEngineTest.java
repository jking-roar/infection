package com.example.funfection;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InfectionEngineTest {

    @Test
    public void shouldMutateReturnsTrueForDeterministicMutationPair() {
        Virus left = virus("left-1", "Spark", 7, 7, 7, "AAA-0");
        Virus right = virus("right-1", "Spark", 7, 7, 7, "AAA-19");

        assertTrue(InfectionEngine.shouldMutate(left, right));
    }

    @Test
    public void shouldMutateReturnsFalseForDeterministicStablePair() {
        Virus left = virus("left-2", "Spark", 7, 7, 7, "AAA-0");
        Virus right = virus("right-2", "Spark", 7, 7, 7, "AAA-0");

        assertFalse(InfectionEngine.shouldMutate(left, right));
    }

    @Test
    public void combineBoostsStatsAndNameWhenMutationOccurs() {
        Virus left = virus("left-3", "Spark", 7, 7, 7, "AAA-0");
        Virus right = virus("right-3", "Spark", 7, 7, 7, "AAA-19");

        Virus result = InfectionEngine.combine(left, right);

        assertTrue(result.hasMutation());
        assertEquals("Spark Chimera", result.getName());
        assertEquals(8, result.getInfectivity());
        assertEquals(7, result.getResilience());
        assertEquals(10, result.getChaos());
    }

    @Test
    public void infectFallsBackToSeededStrainsWhenListsAreEmpty() {
        Virus result = InfectionEngine.infect(Collections.<Virus>emptyList(), Collections.<Virus>emptyList());

        assertTrue(result.getName().endsWith("Remix") || result.getName().endsWith("Chimera"));
        assertTrue(result.getGenome().length() > 0);
    }

    private Virus virus(String id, String family, int infectivity, int resilience, int chaos, String genome) {
        return new Virus(id, family + " Sample", family, "Tester", infectivity, resilience, chaos, false, genome, "Test fixture");
    }
}