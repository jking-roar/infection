package com.example.funfection;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VirusFactoryTest {

    @Test
    public void fromSeedIsDeterministicForSameInputs() {
        Virus first = VirusFactory.fromSeed("Dana", "shared-seed");
        Virus second = VirusFactory.fromSeed("Dana", "shared-seed");

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getName(), second.getName());
        assertEquals(first.getFamily(), second.getFamily());
        assertEquals(first.getGenome(), second.getGenome());
    }

    @Test
    public void fromSeedProducesStatsWithinExpectedBounds() {
        Virus virus = VirusFactory.fromSeed("Dana", "bounded-seed");

        assertTrue(virus.getInfectivity().score() >= 1 && virus.getInfectivity().score() <= 10);
        assertTrue(virus.getResilience().score() >= 1 && virus.getResilience().score() <= 10);
        assertTrue(virus.getChaos().score() >= 1 && virus.getChaos().score() <= 10);
    }

    @Test
    public void parseInviteCodeIgnoresBlankAndInvalidLines() {
        Virus original = new Virus("virus-1", "Spark:Name", "Spark", "Carrier|One",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-123", "Fixture");

        List<Virus> viruses = VirusFactory.parseInviteCode("\n" + original.toShareCode() + "\ninvalid\n");

        assertEquals(1, viruses.size());
        assertEquals("Spark-Name", viruses.get(0).getName());
        assertEquals("Carrier/One", viruses.get(0).getCarrier());
        assertEquals("Imported from invite", viruses.get(0).getOrigin());
    }

    @Test
    public void parseSingleReturnsNullForBrokenInput() {
        assertNull(VirusFactory.parseSingle("too:few:parts"));
        assertNull(VirusFactory.parseSingle("id:family:x:2:3:1:genome:name:carrier"));
    }

    @Test
    public void createStarterVirusesBuildsDefaultCollection() {
        List<Virus> viruses = VirusFactory.createStarterViruses();

        assertEquals(4, viruses.size());
        assertNotNull(viruses.get(0).getId());
        assertFalse(viruses.get(0).getSummaryLine().isEmpty());
    }

    @Test
    public void buildGenomeIncludesMutationMarker() {
        String stableGenome = VirusFactory.buildGenome("12345678-1234-1234-1234-123456789012", "Spark",
            Infectivity.rate(4), Resilience.of(5), Chaos.level(6), false);
        String mutatedGenome = VirusFactory.buildGenome("12345678-1234-1234-1234-123456789012", "Spark",
            Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true);

        assertTrue(stableGenome.endsWith("-S"));
        assertTrue(mutatedGenome.endsWith("-M"));
        assertTrue(mutatedGenome.startsWith("SPA-123456-456"));
    }
}