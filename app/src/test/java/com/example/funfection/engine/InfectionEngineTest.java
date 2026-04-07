package com.example.funfection.engine;

import com.example.funfection.model.Chaos;
import com.example.funfection.model.Infectivity;
import com.example.funfection.model.Resilience;
import com.example.funfection.model.Virus;

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
        Virus left = virus("left-3", "Spark", 7, 7, 7, "AAA-0", 2);
        Virus right = virus("right-3", "Spark", 7, 7, 7, "AAA-19", 3);

        Virus result = InfectionEngine.combine(left, right);

        assertTrue(result.hasMutation());
        assertEquals("Spark Chimera", result.getName());
        assertEquals(Infectivity.rate(9), result.getInfectivity());
        assertEquals(Resilience.of(7), result.getResilience());
        assertEquals(Chaos.level(10), result.getChaos());
        // infectionCount = left(2) + right(3) + 1 committed event = 6
        assertEquals(6, result.getInfectionCount());
    }

    @Test
    public void combineKeepsFamilyAndStableSuffixWhenMutationDoesNotOccur() {
        Virus left = virus("left-4", "Spark", 7, 7, 7, "AAA-0", 0);
        Virus right = virus("right-4", "Spark", 7, 7, 7, "AAA-0", 0);

        Virus result = InfectionEngine.combine(left, right);

        assertFalse(result.hasMutation());
        assertEquals("Spark", result.getFamily());
        assertEquals("Spark Remix", result.getName());
        assertEquals(Infectivity.rate(8), result.getInfectivity());
        assertEquals(Resilience.of(7), result.getResilience());
        assertEquals(Chaos.level(8), result.getChaos());
        assertEquals("Tester x Tester", result.getCarrier());
        // infectionCount = left(0) + right(0) + 1 committed event = 1
        assertEquals(1, result.getInfectionCount());
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
        Virus ownedFirst = new Virus("owned-1", "Spark One", "Spark", "Owner",
            Infectivity.rate(9), Resilience.of(6), Chaos.level(3), false, "OWN-1", "Fixture");
        Virus ownedSecond = new Virus("owned-2", "Spark Two", "Spark", "Owner",
            Infectivity.rate(3), Resilience.of(6), Chaos.level(9), true, "OWN-2", "Fixture");
        Virus friend = virus("friend-1", "Spark", 7, 7, 7, "AAA-0");

        Virus result = InfectionEngine.infect(Arrays.asList(ownedFirst, ownedSecond), Collections.singletonList(friend));

        assertEquals("Spark", result.getFamily());
        assertTrue(result.getName().startsWith("Spark "));
        assertTrue(result.getOrigin().contains("Spark Cluster"));
    }

    @Test
    public void infectLocalUsesOnlyOwnedSelectionAndMarksLocalOrigin() {
        Virus first = virus("owned-local-1", "Spark", 9, 5, 3, "OWN-L1", 2);
        Virus second = virus("owned-local-2", "Spark", 3, 7, 9, "OWN-L2", 4);

        Virus result = InfectionEngine.infectLocal(Arrays.asList(first, second));

        assertEquals("Spark", result.getFamily());
        assertEquals("Spark Local Mix", result.getName());
        assertEquals("Combined from local strains", result.getOrigin());
        assertEquals(Infectivity.rate(6), result.getInfectivity());
        assertEquals(Resilience.of(6), result.getResilience());
        assertEquals(Chaos.level(6), result.getChaos());
        // sum(parent counts) + one committed combine action
        assertEquals(7, result.getInfectionCount());
    }

    @Test
    public void infectLocalFallsBackWhenSelectionIsEmpty() {
        Virus result = InfectionEngine.infectLocal(Collections.<Virus>emptyList());

        assertEquals("Combined from local strains", result.getOrigin());
        assertTrue(result.getName().endsWith(" Local Mix"));
        assertTrue(result.getGenome().length() > 0);
        assertEquals(1, result.getInfectionCount());
    }

    private Virus virus(String id, String family, int infectivity, int resilience, int chaos, String genome) {
        return virus(id, family, infectivity, resilience, chaos, genome, 0);
    }

    private Virus virus(String id, String family, int infectivity, int resilience, int chaos, String genome, int infectionCount) {
        return new Virus(id, family + " Sample", family, "Tester",
                Infectivity.rate(infectivity),
                Resilience.of(resilience),
                Chaos.level(chaos),
                false,
                genome,
                "Test fixture",
                infectionCount);
    }
}