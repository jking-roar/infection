package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.engine.InfectionEngine;
import com.kingjoshdavid.funfection.model.Chaos;
import com.kingjoshdavid.funfection.model.Infectivity;
import com.kingjoshdavid.funfection.model.Resilience;
import com.kingjoshdavid.funfection.model.Virus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VirusRepositoryTest {

    @Before
    public void clearRepositoryBeforeTest() throws Exception {
        clearRepository();
    }

    @After
    public void clearRepositoryAfterTest() throws Exception {
        clearRepository();
    }

    @Test
    public void ensureSeededAddsDefaultVirusesOnlyOnce() {
        VirusRepository.ensureSeeded();
        List<Virus> first = VirusRepository.getViruses();

        VirusRepository.ensureSeeded();
        List<Virus> second = VirusRepository.getViruses();

        assertEquals(4, first.size());
        assertEquals(first.size(), second.size());
        for (Virus virus : first) {
            assertTrue(virus.getInfectionCount() >= 1 && virus.getInfectionCount() <= 20);
        }
    }

    @Test
    public void addVirusPlacesVirusAtFrontOfCollection() {
        VirusRepository.ensureSeeded();
        Virus added = new Virus("added-1", "Custom", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(2), Chaos.level(3), false, "GEN-1", "Fixture");

        VirusRepository.addVirus(added);

        assertEquals("added-1", VirusRepository.getViruses().get(0).getId());
    }

    @Test
    public void getVirusByIdReturnsMatchingVirusOrNull() {
        VirusRepository.ensureSeeded();
        Virus existing = VirusRepository.getViruses().get(0);

        assertNotNull(VirusRepository.getVirusById(existing.getId()));
        assertNull(VirusRepository.getVirusById("missing-id"));
    }

    @Test
    public void pickByIdsReturnsMatchesInRequestedOrder() {
        Virus first = new Virus("first", "First", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-1", "Fixture");
        Virus second = new Virus("second", "Second", "Echo", "Tester",
            Infectivity.rate(2), Resilience.of(2), Chaos.level(2), false, "GEN-2", "Fixture");

        VirusRepository.addVirus(first);
        VirusRepository.addVirus(second);

        List<Virus> picked = VirusRepository.pickByIds(Arrays.asList("first", "missing", "second"));

        assertEquals(2, picked.size());
        assertEquals("first", picked.get(0).getId());
        assertEquals("second", picked.get(1).getId());
    }

    @Test
    public void incrementInfectionCountsIncreasesCountForMatchedIds() {
        Virus v = new Virus("inc-1", "Inc", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-I", "Fixture");
        VirusRepository.addVirus(v);

        VirusRepository.incrementInfectionCounts(Arrays.asList("inc-1"));

        assertEquals(1, VirusRepository.getVirusById("inc-1").getInfectionCount());
    }

    @Test
    public void incrementInfectionCountsIgnoresMissingIds() {
        Virus v = new Virus("safe-1", "Safe", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-S", "Fixture");
        VirusRepository.addVirus(v);

        VirusRepository.incrementInfectionCounts(Arrays.asList("not-here"));

        assertEquals(0, VirusRepository.getVirusById("safe-1").getInfectionCount());
    }

    @Test
    public void replaceVirusUpdatesExistingEntryById() {
        Virus original = new Virus("rep-1", "Rep", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-R", "Fixture");
        VirusRepository.addVirus(original);

        Virus updated = original.incrementInfectionCount();
        VirusRepository.replaceVirus(updated);

        assertEquals(1, VirusRepository.getVirusById("rep-1").getInfectionCount());
        // should not have added a duplicate
        long count = 0;
        for (Virus vv : VirusRepository.getViruses()) {
            if (vv.getId().equals("rep-1")) count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void localCombineKeepsSourceCountsAndCreatesOffspringAtOne() {
        Virus first = new Virus("cmb-1", "First", "Spark", "Tester",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "GEN-C1", "Fixture", 17);
        Virus second = new Virus("cmb-2", "Second", "Echo", "Tester",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "GEN-C2", "Fixture", 5);
        VirusRepository.addVirus(first);
        VirusRepository.addVirus(second);

        Virus offspring = InfectionEngine.infectLocal(Arrays.asList(first, second));
        VirusRepository.addVirus(offspring);

        assertEquals(17, VirusRepository.getVirusById("cmb-1").getInfectionCount());
        assertEquals(5, VirusRepository.getVirusById("cmb-2").getInfectionCount());
        assertEquals(1, VirusRepository.getVirusById(offspring.getId()).getInfectionCount());
    }

    @Test
    public void incrementInfectionCountsOnlyIncrementsSelectedVirusesByOne() {
        Virus selected = new Virus("share-1", "Selected", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-S1", "Fixture", 9);
        Virus notSelected = new Virus("share-2", "Not Selected", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-S2", "Fixture", 4);
        VirusRepository.addVirus(selected);
        VirusRepository.addVirus(notSelected);

        VirusRepository.incrementInfectionCounts(Arrays.asList("share-1"));

        assertEquals(10, VirusRepository.getVirusById("share-1").getInfectionCount());
        assertEquals(4, VirusRepository.getVirusById("share-2").getInfectionCount());
    }

    private void clearRepository() throws Exception {
        Field collectionField = VirusRepository.class.getDeclaredField("COLLECTION");
        collectionField.setAccessible(true);
        List<?> collection = (List<?>) collectionField.get(null);
        collection.clear();
    }
}