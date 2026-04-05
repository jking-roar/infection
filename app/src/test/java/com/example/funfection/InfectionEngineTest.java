package com.example.funfection;

import org.junit.Test;

import java.util.Arrays;
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
        assertEquals(9, result.getInfectivity());
        assertEquals(7, result.getResilience());
        assertEquals(10, result.getChaos());
    }

    @Test
    public void combineKeepsFamilyAndStableSuffixWhenMutationDoesNotOccur() {
        Virus left = virus("left-4", "Spark", 7, 7, 7, "AAA-0");
        Virus right = virus("right-4", "Spark", 7, 7, 7, "AAA-0");

        Virus result = InfectionEngine.combine(left, right);

        assertFalse(result.hasMutation());
        assertEquals("Spark", result.getFamily());
        assertEquals("Spark Remix", result.getName());
        assertEquals(8, result.getInfectivity());
        assertEquals(7, result.getResilience());
        assertEquals(8, result.getChaos());
        assertEquals("Tester x Tester", result.getCarrier());
    }

    @Test
    public void combineMixesFamiliesWhenParentsDiffer() {
        Virus left = virus("left-5", "Spark", 9, 3, 2, "AAA-0");
        Virus right = virus("right-5", "Echo", 1, 9, 10, "BBB-0");

        Virus result = InfectionEngine.combine(left, right);

        assertEquals("Spho", result.getFamily());
        assertTrue(result.getName().startsWith("Spho "));
        assertEquals("Infected from Spark Sample and Echo Sample", result.getOrigin());
    }

    @Test
    public void infectFallsBackToSeededStrainsWhenListsAreEmpty() {
        Virus result = InfectionEngine.infect(Collections.<Virus>emptyList(), Collections.<Virus>emptyList());

        assertTrue(result.getName().endsWith("Remix") || result.getName().endsWith("Chimera"));
        assertTrue(result.getGenome().length() > 0);
    }

    @Test
    public void infectCollapsesOwnedVirusesBeforeCombining() {
        Virus ownedFirst = new Virus("owned-1", "Spark One", "Spark", "Owner", 9, 6, 3, false, "OWN-1", "Fixture");
        Virus ownedSecond = new Virus("owned-2", "Spark Two", "Spark", "Owner", 3, 6, 9, true, "OWN-2", "Fixture");
        Virus friend = virus("friend-1", "Spark", 7, 7, 7, "AAA-0");

        Virus result = InfectionEngine.infect(Arrays.asList(ownedFirst, ownedSecond), Collections.singletonList(friend));

        assertEquals("Spark", result.getFamily());
        assertTrue(result.getName().startsWith("Spark "));
        assertTrue(result.getOrigin().contains("Spark Cluster"));
    }

    private Virus virus(String id, String family, int infectivity, int resilience, int chaos, String genome) {
        return new Virus(id, family + " Sample", family, "Tester", infectivity, resilience, chaos, false, genome, "Test fixture");
    }
}