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
            assertEquals(1, virus.getGeneration());
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
    public void replaceVirusUpdatesExistingEntryById() {
        Virus original = new Virus("rep-1", "Rep", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-R", "Fixture");
        VirusRepository.addVirus(original);

        Virus updated = original.incrementGeneration();
        VirusRepository.replaceVirus(updated);

        assertEquals(2, VirusRepository.getVirusById("rep-1").getGeneration());
        // should not have added a duplicate
        long count = 0;
        for (Virus vv : VirusRepository.getViruses()) {
            if (vv.getId().equals("rep-1")) count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void removeVirusByIdRemovesMatchingEntryAndPreservesRemainingOrder() {
        Virus first = new Virus("rm-1", "First", "Spark", "Tester",
            Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-R1", "Fixture");
        Virus second = new Virus("rm-2", "Second", "Echo", "Tester",
            Infectivity.rate(2), Resilience.of(2), Chaos.level(2), false, "GEN-R2", "Fixture");
        Virus third = new Virus("rm-3", "Third", "Nova", "Tester",
            Infectivity.rate(3), Resilience.of(3), Chaos.level(3), false, "GEN-R3", "Fixture");
        VirusRepository.addVirus(first);
        VirusRepository.addVirus(second);
        VirusRepository.addVirus(third);

        assertTrue(VirusRepository.removeVirusById("rm-2"));

        List<Virus> remaining = VirusRepository.getViruses();
        assertEquals("rm-3", remaining.get(0).getId());
        assertEquals("rm-1", remaining.get(1).getId());
        assertNull(VirusRepository.getVirusById("rm-2"));
    }

    @Test
    public void removeVirusByIdReturnsFalseWhenVirusIsMissing() {
        VirusRepository.ensureSeeded();

        assertTrue(!VirusRepository.getViruses().isEmpty());
        org.junit.Assert.assertFalse(VirusRepository.removeVirusById("missing-id"));
    }

    @Test
    public void localCombineKeepsSourceGenerationsAndCreatesOffspringAtNextGeneration() {
        Virus first = new Virus("cmb-1", "First", "Spark", "Tester",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "GEN-C1", "Fixture", 17);
        Virus second = new Virus("cmb-2", "Second", "Echo", "Tester",
            Infectivity.rate(5), Resilience.of(5), Chaos.level(5), false, "GEN-C2", "Fixture", 5);
        VirusRepository.addVirus(first);
        VirusRepository.addVirus(second);

        Virus offspring = InfectionEngine.infectLocal(Arrays.asList(first, second));
        VirusRepository.addVirus(offspring);

        assertEquals(17, VirusRepository.getVirusById("cmb-1").getGeneration());
        assertEquals(5, VirusRepository.getVirusById("cmb-2").getGeneration());
        assertEquals(18, VirusRepository.getVirusById(offspring.getId()).getGeneration());
    }

    private void clearRepository() throws Exception {
        Field collectionField = VirusRepository.class.getDeclaredField("COLLECTION");
        collectionField.setAccessible(true);
        List<?> collection = (List<?>) collectionField.get(null);
        //noinspection DataFlowIssue
        collection.clear();
    }
}