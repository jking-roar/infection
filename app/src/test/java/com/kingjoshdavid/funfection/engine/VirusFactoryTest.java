package com.kingjoshdavid.funfection.engine;

import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class VirusFactoryTest {

    @Before
    public void setUp() {
        UserProfileRepository.setCurrentUserForTesting(new UserProfile("user-1", "Quiet Otter"));
    }

    @After
    public void tearDown() {
        UserProfileRepository.resetForTesting();
    }

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
        String seed = "café-Δna";

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
        assertEquals("Quiet Otter", trimmed.getCarrier());
        assertEquals("Seeded in lab", trimmed.getOrigin());
        assertTrue(trimmed.getOriginInfo().hasDirectSource());
        assertEquals("user-1", trimmed.getOriginInfo().getDirectSource().getId());
    }

    @Test
    public void createLabVirusFallsBackToRandomSeedWhenBlank() {
        Virus first = VirusFactory.createLabVirus("   ");
        Virus second = VirusFactory.createLabVirus(null);

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
        assertEquals("Quiet Otter", first.getCarrier());
        assertEquals("Quiet Otter", second.getCarrier());
    }

    @Test
    public void createRandomFriendVirusUsesFriendFallbackOrigin() {
        Virus friend = VirusFactory.createRandomFriendVirus();

        assertTrue(friend.getCarrier().contains("[SIMULATED]"));
        assertEquals("Generated as random friend fallback", friend.getOrigin());
        assertFalse(friend.getOriginInfo().isRealFriendSource());
        assertEquals(0, friend.getOriginInfo().getDegreeOfSeparation());
    }

    @Test
    public void parseInviteCodeIgnoresBlankAndInvalidLines() {
        Virus original = new Virus("virus-1", "Spark:Name", "Spark", "Carrier|One",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-123",
                VirusOrigin.infectedFrom("Local", VirusOrigin.seededInLab(), "Carrier One",
                        VirusOrigin.importedFromInvite(VirusOrigin.randomFriendFallback("Placeholder [SIMULATED]"), "Carrier One")),
                7);

        List<Virus> viruses = VirusFactory.parseInviteCode("\n" + original.toShareCode() + "\ninvalid\n");

        assertEquals(1, viruses.size());
        assertEquals("Spark-Name", viruses.get(0).getName());
        assertEquals("Carrier/One", viruses.get(0).getCarrier());
        assertEquals("Imported from invite", viruses.get(0).getOrigin());
        assertEquals(7, viruses.get(0).getGeneration());
    }

    @Test
    public void parseInviteCodePreservesOriginPayloadAndAdvancesDegrees() {
        Virus shared = new Virus("virus-2", "Shared Sample", "Spark", "Jordan",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), false, "GEN-777",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Jordan"), 1);

        List<Virus> viruses = VirusFactory.parseInviteCode(shared.toShareCode());

        assertEquals(1, viruses.size());
        assertEquals("Imported from invite", viruses.get(0).getOrigin());
        assertTrue(viruses.get(0).getOriginInfo().hasDirectSource());
        assertTrue(viruses.get(0).getOriginInfo().isRealFriendSource());
        assertEquals(1, viruses.get(0).getOriginInfo().getDegreeOfSeparation());
        assertEquals(1, viruses.get(0).getOriginInfo().getPatientZeros().size());
        assertEquals(1, viruses.get(0).getOriginInfo().getPatientZeros().get(0).getDegreeOfSeparation());
    }

    @Test
    public void parseInviteCodeRetainsPrimaryAndSecondaryPatientZerosFromSharedOriginOrder() {
        Virus shared = new Virus("virus-order-1", "Shared Sample", "Spark", "Jordan",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), false, "GEN-778",
                VirusOrigin.infectedFrom("Left", VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Creator"),
                        "Right", VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "RightZero")),
                3);

        List<Virus> viruses = VirusFactory.parseInviteCode(shared.toShareCode());

        assertEquals(1, viruses.size());
        assertEquals(2, viruses.get(0).getOriginInfo().getPatientZeros().size());
        assertEquals("Creator", viruses.get(0).getOriginInfo().getPatientZeros().get(0).getDisplayName());
        assertEquals("RightZero", viruses.get(0).getOriginInfo().getPatientZeros().get(1).getDisplayName());
    }

    @Test
    public void parseInviteCodeDoesNotStoreFullInvitePayloadInRawSeed() {
        Virus shared = new Virus("virus-seed-1", "Shared Sample", "Spark", "Jordan",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), false, "GEN-779",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Jordan"), 1);

        Virus parsed = VirusFactory.parseInviteCode(shared.toShareCode()).get(0);

        assertEquals("invite-id:virus-seed-1", parsed.getRawSeed());
        assertFalse(parsed.getRawSeed().contains(shared.toShareCode()));
    }

    @Test
    public void parseInviteCodeAcceptsSharedMessageBodyAroundTheCodes() {
        Virus first = new Virus("virus-qr-1", "Shared One", "Spark", "Jordan",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), false, "GEN-701",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Jordan"), 1);
        Virus second = new Virus("virus-qr-2", "Shared Two", "Echo", "Taylor",
                Infectivity.rate(6), Resilience.of(4), Chaos.level(5), true, "GEN-702",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Taylor"), 2);

        String sharedBody = "Swap strains with me in Funfection. Paste this invite code into the lab:\n\n"
                + first.toShareCode() + "\n" + second.toShareCode();

        List<Virus> viruses = VirusFactory.parseInviteCode(sharedBody);

        assertEquals(2, viruses.size());
        assertEquals(first.getId(), viruses.get(0).getId());
        assertEquals(second.getId(), viruses.get(1).getId());
    }

    @Test
    public void parseInviteCodeDefaultsLegacyNineFieldCodesToGenerationOne() {
        // A legacy 9-field share code without trailing generation
        String legacyCode = "virus-L:Spark:4:5:6:1:GEN-123:LegacyName:LegacyCarrier";

        List<Virus> viruses = VirusFactory.parseInviteCode(legacyCode);

        assertEquals(1, viruses.size());
        assertEquals(1, viruses.get(0).getGeneration());
    }

    @Test
    public void parseSingleReturnsNullForBrokenInput() {
        assertNull(VirusFactory.parseSingle("too:few:parts"));
        assertNull(VirusFactory.parseSingle("id:family:x:2:3:1:genome:name:carrier"));
    }

    @Test
    public void createWildVirusFromQrSetsWildQrOriginAndSeed() {
        String seed = "https://example.com/product/12345";

        Virus virus = VirusFactory.createWildVirus(seed, true);

        assertEquals("Found in the wild (QR code)", virus.getOrigin());
        assertEquals(SeedUtil.seedFromString(seed), virus.getSeed());
        assertNotNull(virus.getId());
        assertEquals("Quiet Otter", virus.getCarrier());
        assertFalse(virus.getOriginInfo().getPatientZeros().isEmpty());
        assertEquals("user-1", virus.getOriginInfo().getPatientZeros().get(0).getId());
    }

    @Test
    public void createWildVirusFromBarcodeSetsWildBarcodeOriginAndSeed() {
        String seed = "9780201633610";

        Virus virus = VirusFactory.createWildVirus(seed, false);

        assertEquals("Found in the wild (barcode)", virus.getOrigin());
        assertEquals(SeedUtil.seedFromString(seed), virus.getSeed());
        assertNotNull(virus.getId());
        assertFalse(virus.getOriginInfo().getPatientZeros().isEmpty());
        assertEquals("user-1", virus.getOriginInfo().getPatientZeros().get(0).getId());
    }

    @Test
    public void parseSingleBackfillsPatientZeroWhenInviteHasNoOriginPayload() {
        Virus parsed = VirusFactory.parseSingle("legacy-id:Spark:4:5:6:0:GEN-123:Legacy Name:LegacyCarrier:1");

        assertNotNull(parsed);
        assertFalse(parsed.getOriginInfo().getPatientZeros().isEmpty());
        assertEquals("LegacyCarrier", parsed.getOriginInfo().getPatientZeros().get(0).getDisplayName());
    }

    @Test
    public void createWildVirusIsDeterministicForSameSeed() {
        String seed = "deterministic-wild-seed";

        Virus first = VirusFactory.createWildVirus(seed, true);
        Virus second = VirusFactory.createWildVirus(seed, true);

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getName(), second.getName());
        assertEquals(first.getFamily(), second.getFamily());
    }

    @Test
    public void createWildVirusFallsBackToRandomSeedWhenBlank() {
        Virus fromNull = VirusFactory.createWildVirus(null, true);
        Virus fromBlank = VirusFactory.createWildVirus("   ", false);

        assertNotNull(fromNull.getId());
        assertNotNull(fromBlank.getId());
        assertNotEquals(fromNull.getId(), fromBlank.getId());
    }

    @Test
    public void createStarterVirusesBuildsDefaultCollection() {
        List<Virus> first = VirusFactory.createStarterViruses();
        List<Virus> second = VirusFactory.createStarterViruses();

        assertEquals(4, first.size());
        assertNotNull(first.get(0).getId());
        assertFalse(first.get(0).getSummaryLine().isEmpty());

        for (int i = 0; i < first.size(); i++) {
            int firstGeneration = first.get(i).getGeneration();
            int secondGeneration = second.get(i).getGeneration();
            assertEquals(1, firstGeneration);
            assertEquals(firstGeneration, secondGeneration);
        }
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