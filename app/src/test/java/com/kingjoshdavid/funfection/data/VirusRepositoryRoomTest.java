package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Chaos;
import com.kingjoshdavid.funfection.model.Friend;
import com.kingjoshdavid.funfection.model.Infectivity;
import com.kingjoshdavid.funfection.model.Resilience;
import com.kingjoshdavid.funfection.model.UserProfile;
import com.kingjoshdavid.funfection.model.Virus;
import com.kingjoshdavid.funfection.model.VirusOrigin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VirusRepositoryRoomTest {

    private FunfectionDatabase database;

    @Before
    public void setUp() throws Exception {
        database = RepositoryRoomTestSupport.setUpInMemoryDatabase();
    }

    @After
    public void tearDown() throws Exception {
        RepositoryRoomTestSupport.tearDownInMemoryDatabase(database);
    }

    @Test
    public void ensureSeededStoresStarterVirusesInRoomPath() {
        assertNotNull(DatabaseProvider.getIfInitialized());

        VirusRepository.ensureSeeded();
        List<Virus> viruses = VirusRepository.getViruses();

        assertEquals(4, viruses.size());
        for (Virus virus : viruses) {
            assertEquals(1, virus.getGeneration());
        }
    }

    @Test
    public void addAndReplacePersistThroughRoomWithNewestFirstOrdering() throws Exception {
        Virus first = new Virus("room-1", "First", "Spark", "Tester",
                Infectivity.rate(2), Resilience.of(3), Chaos.level(4), false, "GEN-ROOM-1", "Fixture");
        Virus second = new Virus("room-2", "Second", "Echo", "Tester",
                Infectivity.rate(4), Resilience.of(5), Chaos.level(6), true, "GEN-ROOM-2", "Fixture");

        VirusRepository.addVirus(first);
        Thread.sleep(2L);
        VirusRepository.addVirus(second);

        List<Virus> viruses = VirusRepository.getViruses();
        int firstIndex = -1;
        int secondIndex = -1;
        for (int i = 0; i < viruses.size(); i++) {
            if ("room-1".equals(viruses.get(i).getId())) {
                firstIndex = i;
            }
            if ("room-2".equals(viruses.get(i).getId())) {
                secondIndex = i;
            }
        }
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex >= 0);
        assertTrue(secondIndex < firstIndex);

        Virus updated = second.incrementGeneration();
        VirusRepository.replaceVirus(updated);

        assertEquals(2, VirusRepository.getVirusById("room-2").getGeneration());
        int count = 0;
        for (Virus virus : VirusRepository.getViruses()) {
            if ("room-2".equals(virus.getId())) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    public void pickByIdsAndPurgeByIdUseRoomData() {
        Virus first = new Virus("room-pick-1", "First", "Spark", "Tester",
                Infectivity.rate(1), Resilience.of(1), Chaos.level(1), false, "GEN-P1", "Fixture");
        Virus second = new Virus("room-pick-2", "Second", "Echo", "Tester",
                Infectivity.rate(2), Resilience.of(2), Chaos.level(2), false, "GEN-P2", "Fixture");
        VirusRepository.addVirus(first);
        VirusRepository.addVirus(second);

        List<Virus> picked = VirusRepository.pickByIds(Arrays.asList("room-pick-1", "missing", "room-pick-2"));
        assertEquals(2, picked.size());
        assertEquals("room-pick-1", picked.get(0).getId());
        assertEquals("room-pick-2", picked.get(1).getId());

        assertEquals(VirusRepository.PurgeResult.REMOVED, VirusRepository.getPurgeStatus("room-pick-1"));
        assertEquals(VirusRepository.PurgeResult.REMOVED, VirusRepository.purgeVirusById("room-pick-1"));
        assertNull(VirusRepository.getVirusById("room-pick-1"));
        assertEquals(VirusRepository.PurgeResult.MISSING, VirusRepository.purgeVirusById("room-pick-1"));
    }

    @Test
    public void addAndReplacePersistDiscoveredFriendsThroughRoom() {
        Virus first = new Virus("room-disc-1", "First", "Spark", "Jordan",
                Infectivity.rate(3), Resilience.of(3), Chaos.level(3), false, "GEN-RD1",
                originWithDirectSource("vector-room-1", "Jordan"), 1);
        Virus updated = new Virus("room-disc-1", "First", "Spark", "Jordan",
                Infectivity.rate(3), Resilience.of(3), Chaos.level(3), false, "GEN-RD1",
                originWithDirectSource("vector-room-1", "Jordan Prime"), 2);

        VirusRepository.addVirus(first);
        VirusRepository.replaceVirus(updated);

        Friend discovered = FriendsRepository.getFriendById("vector-room-1");
        assertNotNull(discovered);
        assertEquals("Jordan Prime", discovered.getDisplayName());
        assertEquals(1, discovered.getUsernameHistory().size());
        assertEquals("Jordan", discovered.getUsernameHistory().get(0).getUsername());
    }

    @Test
    public void addVirusWithUnknownOriginFriendIdsSkipsUnknownLinksAndDoesNotCrash() {
        UserProfile currentUser = UserProfileRepository.getCurrentUser();
        FriendsRepository.ensureCurrentUserFriendExists();

        Virus virus = new Virus("room-origin-link-1", "Link", "Proof", "Tester",
                Infectivity.rate(3), Resilience.of(4), Chaos.level(5), false, "GEN-LINK-1",
                originWithDirectSourceAndPatientZero("missing-friend-id", "Missing Friend", false,
                        currentUser.getId(), currentUser.getUserName()), 2);

        VirusRepository.addVirus(virus);

        List<Virus> selfLinkedViruses = VirusRepository.getVirusesByFriendId(currentUser.getId());
        assertEquals(1, selfLinkedViruses.size());
        assertEquals("room-origin-link-1", selfLinkedViruses.get(0).getId());
        assertTrue(VirusRepository.getVirusesByFriendId("missing-friend-id").isEmpty());
    }

    private VirusOrigin originWithDirectSource(String id, String displayName) {
        String raw = "1\nIMPORTED_INVITE\nImported from invite\n"
                + id + "\n"
                + displayName + "\n"
                + "1\n1\n0\n";
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return VirusOrigin.fromSharePayload(payload);
    }

    private VirusOrigin originWithDirectSourceAndPatientZero(String directId,
                                                             String directName,
                                                             boolean directRealFriend,
                                                             String patientZeroId,
                                                             String patientZeroName) {
        String raw = "1\nIMPORTED_INVITE\nImported from invite\n"
                + directId + "\n"
                + directName + "\n"
                + (directRealFriend ? "1" : "0") + "\n1\n1\n"
                + patientZeroId + "\t" + patientZeroName + "\t2\n";
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return VirusOrigin.fromSharePayload(payload);
    }
}

