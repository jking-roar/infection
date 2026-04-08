package com.example.funfection.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VirusTest {

    @Test
    public void toShareCodeSanitizesReservedCharacters() {
        Virus virus = new Virus("virus-1", "Spark:Name", "Spark", "Carrier|One",
            Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-123", "Fixture", 3);

        String shareCode = virus.toShareCode();

        assertTrue(shareCode.startsWith("virus-1:Spark:4:5:6:1:GEN-123:Spark-Name:Carrier/One:3:"));
        String[] pieces = shareCode.split(":");
        assertEquals(11, pieces.length);
        assertNotNull(VirusOrigin.fromSharePayload(pieces[10]));
    }

    @Test
    public void getSummaryLineIncludesMutationLabelAndRate() {
        Virus virus = new Virus("virus-2", "Spark Name", "Spark", "Carrier",
            Infectivity.rate(9), Resilience.of(8), Chaos.level(8), true, "GEN-999", "Fixture", 4);

        String summary = virus.getSummaryLine();

        assertTrue(summary.contains("Mutated"));
        assertTrue(summary.contains("Rate OUTBREAK"));
        assertTrue(summary.contains("Infections 4"));
    }

    @Test
    public void getInfectionRateDelegatesToScoreBands() {
        Virus virus = new Virus("virus-3", "Stable", "Spark", "Carrier",
            Infectivity.rate(4), Resilience.of(4), Chaos.level(4), false, "GEN-444", "Fixture");

        assertEquals(InfectionRates.MEDIUM, virus.getInfectionRate());
    }

    @Test
    public void getInfectionStrengthUsesLegacyDisplayFormula() {
        Virus virus = new Virus("virus-4", "Stable", "Spark", "Carrier",
            Infectivity.rate(8), Resilience.of(5), Chaos.level(2), false, "GEN-852", "Fixture");

        assertEquals(115, virus.getInfectionStrength());
    }

    @Test
    public void incrementInfectionCountReturnsCopiedVirusWithIncrementedCount() {
        Virus original = new Virus("virus-5", "Stable", "Spark", "Carrier",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-111", "Fixture", 2);

        Virus incremented = original.incrementInfectionCount();

        assertEquals(2, original.getInfectionCount());
        assertEquals(3, incremented.getInfectionCount());
        assertEquals(original.getId(), incremented.getId());
    }

    @Test
    public void getInfectionStrengthDoesNotIncludeChaos() {
        // getInfectionStrength uses infectivity*10 + resilience*7 (chaos is excluded).
        // Two viruses that differ only in chaos therefore share the same infection strength,
        // even though getInfectionRate() includes chaos and produces different bands for them.
        Virus lowChaos = new Virus("v-lc", "A", "Spark", "C",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(1), false, "G1", "F");
        Virus highChaos = new Virus("v-hc", "B", "Spark", "C",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(10), false, "G2", "F");

        assertEquals(lowChaos.getInfectionStrength(), highChaos.getInfectionStrength());
        assertNotEquals(lowChaos.getInfectionRate(), highChaos.getInfectionRate());
    }
}
