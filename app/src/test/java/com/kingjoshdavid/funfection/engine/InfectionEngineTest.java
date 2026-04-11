package com.kingjoshdavid.funfection.engine;

import com.kingjoshdavid.funfection.model.Chaos;
import com.kingjoshdavid.funfection.model.Infectivity;
import com.kingjoshdavid.funfection.model.Resilience;
import com.kingjoshdavid.funfection.model.Virus;
import com.kingjoshdavid.funfection.model.VirusOrigin;

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
        assertEquals("Spark Sample", result.getName());
        assertEquals(Infectivity.rate(9), result.getInfectivity());
        assertEquals(Resilience.of(7), result.getResilience());
        assertEquals(Chaos.level(10), result.getChaos());
        assertEquals(4, result.getGeneration());
    }

    @Test
    public void combineKeepsFamilyAndStableSuffixWhenMutationDoesNotOccur() {
        Virus left = virus("left-4", "Spark", 7, 7, 7, "AAA-0", 0);
        Virus right = virus("right-4", "Spark", 7, 7, 7, "AAA-0", 0);

        Virus result = InfectionEngine.combine(left, right);

        assertFalse(result.hasMutation());
        assertEquals("Spark", result.getFamily());
        assertEquals("Spark Sample", result.getName());
        assertEquals(Infectivity.rate(8), result.getInfectivity());
        assertEquals(Resilience.of(7), result.getResilience());
        assertEquals(Chaos.level(8), result.getChaos());
        assertEquals("Tester x Tester", result.getCarrier());
        assertEquals(2, result.getGeneration());
    }

    @Test
    public void combineMixesFamiliesWhenParentsDiffer() {
        Virus left = virus("left-5", "Spark", 9, 3, 2, "AAA-0");
        Virus right = virus("right-5", "Echo", 1, 9, 10, "BBB-0");

        Virus result = InfectionEngine.combine(left, right);

        assertEquals("Spho", result.getFamily());
        assertEquals("Spark Sample", result.getName());
        assertEquals("Infected from Spark Sample and Echo Sample", result.getOrigin());
    }

    @Test
    public void infectFallsBackToSeededStrainsWhenListsAreEmpty() {
        Virus result = InfectionEngine.infect(Collections.emptyList(), Collections.emptyList());

        assertFalse(result.getName().trim().isEmpty());
        assertFalse(result.getGenome().isEmpty());
    }

    @Test
    public void combineUsesLowerInfectionPrefixAndHigherInfectionSuffix() {
        Virus left = new Virus("rule-left", "Alpha", "LeftTail", "Spark", "Tester",
            Infectivity.rate(6), Resilience.of(6), Chaos.level(6), false, "RL-1",
            VirusOrigin.seededInLab(), 1);
        Virus right = new Virus("rule-right", "Omega", "RightTail", "Spark", "Tester",
            Infectivity.rate(6), Resilience.of(6), Chaos.level(6), false, "RR-1",
            VirusOrigin.seededInLab(), 5);

        Virus result = InfectionEngine.combine(left, right);

        assertEquals("Alpha", result.getPrefix());
        assertEquals("RightTail", result.getSuffix());
        assertEquals("Alpha RightTail", result.getName());
    }

    @Test
    public void combineTieUsesLeftPrefixAndRightSuffix() {
        Virus left = new Virus("tie-left", "LeftPrefix", "LeftTail", "Spark", "Tester",
            Infectivity.rate(6), Resilience.of(6), Chaos.level(6), false, "TL-1",
            VirusOrigin.seededInLab(), 4);
        Virus right = new Virus("tie-right", "RightPrefix", "RightTail", "Spark", "Tester",
            Infectivity.rate(6), Resilience.of(6), Chaos.level(6), false, "TR-1",
            VirusOrigin.seededInLab(), 4);

        Virus result = InfectionEngine.combine(left, right);

        assertEquals("LeftPrefix", result.getPrefix());
        assertEquals("RightTail", result.getSuffix());
        assertEquals("LeftPrefix RightTail", result.getName());
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
        assertTrue(result.getOrigin().contains("Spark Two"));
    }

    @Test
    public void infectLocalUsesOnlyOwnedSelectionAndMarksLocalOrigin() {
        Virus first = virus("owned-local-1", "Spark", 9, 5, 3, "OWN-L1", 2);
        Virus second = virus("owned-local-2", "Spark", 3, 7, 9, "OWN-L2", 4);

        Virus result = InfectionEngine.infectLocal(Arrays.asList(first, second));

        assertEquals("Spark", result.getFamily());
        assertEquals("Spark Sample", result.getName());
        assertEquals("Local Mix", result.getProductionContext());
        assertEquals("Combined from local strains", result.getOrigin());
        assertEquals(Infectivity.rate(6), result.getInfectivity());
        assertEquals(Resilience.of(6), result.getResilience());
        assertEquals(Chaos.level(6), result.getChaos());
        assertEquals(5, result.getGeneration());
    }

    @Test
    public void infectLocalFallsBackWhenSelectionIsEmpty() {
        Virus result = InfectionEngine.infectLocal(Collections.emptyList());

        assertEquals("Combined from local strains", result.getOrigin());
        assertFalse(result.getName().trim().isEmpty());
        assertEquals("Local Mix", result.getProductionContext());
        assertFalse(result.getGenome().isEmpty());
        assertEquals(2, result.getGeneration());
    }

    @Test
    public void combineUsesMaxParentGenerationPlusOne() {
        Virus left = virus("left-count-1", "Spark", 6, 6, 6, "AAA-0", 17);
        Virus right = virus("right-count-1", "Echo", 6, 6, 6, "BBB-0", 5);

        Virus result = InfectionEngine.combine(left, right);

        assertEquals(18, result.getGeneration());
    }

    @Test
    public void combineAppliesEnergyBiasWhenStatsAreDivergent() {
        // infectivity diff = |9-1| = 8 and chaos diff = |2-10| = 8, both exceed the close threshold.
        // With preferEnergy=true the weighted formula becomes (left*2+right)/3 + 1.
        // infectivity: (9*2+1)/3 + 1 = 6 + 1 = 7   (resilience uses preferEnergy=false: (3*2+9)/3 = 5)
        // chaos:       (2*2+10)/3 + 1 = 4 + 1 = 5   (no mutation for this pair)
        Virus left = virus("left-div", "Spark", 9, 3, 2, "AAA-0");
        Virus right = virus("right-div", "Echo", 1, 9, 10, "BBB-0");

        Virus result = InfectionEngine.combine(left, right);

        assertFalse(result.hasMutation());
        assertEquals(Infectivity.rate(7), result.getInfectivity());
        assertEquals(Resilience.of(5), result.getResilience());
        assertEquals(Chaos.level(5), result.getChaos());
    }

    @Test
    public void infectLocalWithMixedFamiliesUsesLastFamilyAsCollapsedFamily() {
        // When owned viruses span multiple families, collapse assigns the last virus's family.
        // The collapsed template is named "Hybrid Cluster" but its family field is the last entry's
        // family rather than a hybrid code. infectLocal then uses that family directly.
        Virus first = new Virus("mixed-1", "Spark One", "Spark", "Owner",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "G1", "Fixture");
        Virus second = new Virus("mixed-2", "Echo Two", "Echo", "Owner",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "G2", "Fixture");

        Virus result = InfectionEngine.infectLocal(Arrays.asList(first, second));

        assertEquals("Echo", result.getFamily());
        assertEquals("Spark Two", result.getName());
        assertEquals("Local Mix", result.getProductionContext());
    }

    @Test
    public void infectKeepsFurthestPatientZerosAndResetsKnownDirectFriendToOneDegree() {
        Virus local = new Virus("owned-origin-1", "Local Sample", "Spark", "Owner",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "OWN-O1",
            VirusOrigin.importedFromInvite(
                VirusOrigin.infectedFrom("Seed", VirusOrigin.seededInLab(), "Far Friend",
                    VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Far Friend")),
                "Near Friend"),
            0);
        Virus friend = new Virus("friend-origin-1", "Friend Sample", "Spark", "Near Friend",
            Infectivity.rate(6), Resilience.of(5), Chaos.level(4), false, "FRI-O1",
            VirusOrigin.importedFromInvite(
                VirusOrigin.infectedFrom("Seed", VirusOrigin.seededInLab(), "Far Friend",
                    VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Far Friend")),
                "Near Friend"),
            0);

        Virus result = InfectionEngine.infect(Collections.singletonList(local), Collections.singletonList(friend));

        assertEquals("Infected from Local Sample and Friend Sample", result.getOrigin());
        assertTrue(result.getOriginInfo().hasDirectSource());
        assertEquals(1, result.getOriginInfo().getDegreeOfSeparation());
        assertEquals("Near Friend", result.getOriginInfo().getDirectSource().getDisplayName());
        assertFalse(result.getOriginInfo().getPatientZeros().isEmpty());
        assertEquals(2, result.getOriginInfo().getPatientZeros().get(0).getDegreeOfSeparation());
    }

    @Test
    public void infectWithRandomFriendDoesNotCreatePatientZeroDegree() {
        Virus owned = virus("owned-sim-1", "Spark", 5, 5, 5, "OWN-S1", 0);
        Virus fakeFriend = VirusFactory.createRandomFriendVirus();

        Virus result = InfectionEngine.infect(Collections.singletonList(owned), Collections.singletonList(fakeFriend));

        assertTrue(result.getOriginInfo().hasDirectSource());
        assertFalse(result.getOriginInfo().isRealFriendSource());
        assertEquals(0, result.getOriginInfo().getDegreeOfSeparation());
        assertTrue(result.getOriginInfo().getPatientZeros().isEmpty());
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