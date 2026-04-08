package com.example.funfection.engine;

import com.example.funfection.model.Chaos;
import com.example.funfection.model.Infectivity;
import com.example.funfection.model.Resilience;
import com.example.funfection.model.Virus;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    public void fromSeedUsesUtf8ForDeterministicUuidDerivation() {
        String seed = "caf\u00e9-b4na";

        Virus virus = VirusFactory.fromSeed("Dana", seed);

        assertEquals(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString(), virus.getId());
    }

    @Test
    public void fromSeedProducesStatsWithinExpectedBounds() {
        Virus virus = VirusFactory.fromSeed("Dana", "bounded-seed");

        assertTrue(virus.getInfectivity().score() >= 1 && virus.getInfectivity().score() <= 10);
        assertTrue(virus.getResilience().score() >= 1 && virus.getResilience().score() <= 10);
        assertTrue(virus.getChaos().score() >= 1 && virus.getChaos().score() <= 10);
    }

    @Test
    public void createLabVirusUsesTrimmedSeedDeterministically() {
        Virus trimmed = VirusFactory.createLabVirus("  custom-seed  ");
        Virus plain = VirusFactory.createLabVirus("custom-seed");

        assertEquals(trimmed.getId(), plain.getId());
        assertEquals(trimmed.getName(), plain.getName());
        assertEquals("Lab", trimmed.getCarrier());
        assertEquals("Seeded in lab", trimmed.getOrigin());
    }

    @Test
    public void createLabVirusFallsBackToRandomSeedWhenBlank() {
        Virus first = VirusFactory.createLabVirus("   ");
        Virus second = VirusFactory.createLabVirus(null);

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
        assertEquals("Lab", first.getCarrier());
        assertEquals("Lab", second.getCarrier());
    }

    @Test
    public void createRandomFriendVirusUsesFriendFallbackOrigin() {
        Virus friend = VirusFactory.createRandomFriendVirus();

        assertTrue(friend.getCarrier().startsWith("Friend-"));
        assertEquals("Generated as random friend fallback", friend.getOrigin());
    }

    @Test
    public void parseInviteCodeIgnoresBlankAndInvalidLines() {
        Virus original = new Virus("virus-1", "Spark:Name", "Spark", "Carrier|One",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-123", "Fixture", 7);

        List<Virus> viruses = VirusFactory.parseInviteCode("\n" + original.toShareCode() + "\ninvalid\n");

        assertEquals(1, viruses.size());
        assertEquals("Spark-Name", viruses.get(0).getName());
        assertEquals("Carrier/One", viruses.get(0).getCarrier());
        assertEquals("Imported from invite", viruses.get(0).getOrigin());
        assertEquals(7, viruses.get(0).getInfectionCount());
    }

    @Test
    public void parseInviteCodeDefaultsLegacyNineFieldCodesToZeroCount() {
        // A legacy 9-field share code without trailing infectionCount
        String legacyCode = "virus-L:Spark:4:5:6:1:GEN-123:LegacyName:LegacyCarrier";

        List<Virus> viruses = VirusFactory.parseInviteCode(legacyCode);

        assertEquals(1, viruses.size());
        assertEquals(0, viruses.get(0).getInfectionCount());
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