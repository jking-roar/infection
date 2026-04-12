package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Friend;
import com.kingjoshdavid.funfection.model.UsernameHistoryEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FriendsRepositoryRoomTest {

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
    public void saveFriendAndGetFriendByIdRoundTripThroughRoom() {
        assertNotNull(DatabaseProvider.getIfInitialized());

        Friend friend = new Friend("friend-1", "Alpha", "code-1");
        FriendsRepository.saveFriend(friend);

        Friend loaded = FriendsRepository.getFriendById("friend-1");
        assertNotNull(loaded);
        assertEquals("Alpha", loaded.getDisplayName());
        assertEquals("code-1", loaded.getInviteCode());
    }

    @Test
    public void getFriendsReturnsNewestFirstAndUpdateKeepsSingleRow() throws Exception {
        Friend first = new Friend("friend-a", "A", "invite-a");
        Friend second = new Friend("friend-b", "B", "invite-b");

        FriendsRepository.saveFriend(first);
        Thread.sleep(2L);
        FriendsRepository.saveFriend(second);

        List<Friend> ordered = FriendsRepository.getFriends();
        assertEquals("friend-b", ordered.get(0).getId());
        assertEquals("friend-a", ordered.get(1).getId());

        FriendsRepository.saveFriend(new Friend("friend-a", "A Updated", "invite-a2"));

        List<Friend> afterUpdate = FriendsRepository.getFriends();
        int countA = 0;
        for (Friend friend : afterUpdate) {
            if ("friend-a".equals(friend.getId())) {
                countA++;
                assertEquals("A Updated", friend.getDisplayName());
            }
        }
        assertEquals(1, countA);
    }

    @Test
    public void deleteFriendAndPickByIdsUseRoomData() {
        Friend first = new Friend("friend-pick-1", "One", "invite-1");
        Friend second = new Friend("friend-pick-2", "Two", "invite-2");
        FriendsRepository.saveFriend(first);
        FriendsRepository.saveFriend(second);

        List<Friend> picked = FriendsRepository.pickByIds(Arrays.asList("friend-pick-2", "missing", "friend-pick-1"));
        assertEquals(2, picked.size());
        assertEquals("friend-pick-2", picked.get(0).getId());
        assertEquals("friend-pick-1", picked.get(1).getId());

        assertTrue(FriendsRepository.deleteFriend("friend-pick-1"));
        assertNull(FriendsRepository.getFriendById("friend-pick-1"));
        assertFalse(FriendsRepository.deleteFriend("friend-pick-1"));
    }

    @Test
    public void saveFriendRoundTripsHistoryAndProtectedMetadata() {
        Friend scientist = new Friend("scientist-room", "Professor Tesla [SIMULATED]", "",
                "", "private notes should be cleared", "Scientist fallback", true,
                Collections.singletonList(new UsernameHistoryEntry("Professor Tesla", 1000L)));

        FriendsRepository.saveFriend(scientist);

        Friend loaded = FriendsRepository.getFriendById("scientist-room");
        assertNotNull(loaded);
        assertTrue(loaded.isProtectedProfile());
        assertEquals("Scientist fallback", loaded.getDescription());
        assertEquals(1, loaded.getUsernameHistory().size());
        assertEquals("Professor Tesla", loaded.getUsernameHistory().get(0).getUsername());
        assertEquals("", loaded.getNotes());
        assertFalse(FriendsRepository.deleteFriend("scientist-room"));
    }

    @Test
    public void initializeSeedsKnownScientistsBeforeDiscovery() {
        FriendsRepository.initialize(ApplicationProvider.getApplicationContext());

        List<Friend> friends = FriendsRepository.getFriends();
        assertEquals(5, friends.size());

        Friend tesla = findFriendByDisplayName("Professor Tesla");
        assertNotNull(tesla);
        assertTrue(tesla.isProtectedProfile());
        assertFalse(tesla.getDescription().isEmpty());
        assertFalse(FriendsRepository.deleteFriend(tesla.getId()));
    }

    @Test
    public void usernameHistoryTimestampPersistedAndReturnedOnFetch() {
        long ts = 999_000L;
        Friend friend = new Friend("friend-ts", "Alice", "code-ts",
                "", "", "", false,
                Collections.singletonList(new UsernameHistoryEntry("OldAlice", ts)));
        FriendsRepository.saveFriend(friend);

        Friend loaded = FriendsRepository.getFriendById("friend-ts");
        assertNotNull(loaded);
        assertEquals(1, loaded.getUsernameHistory().size());
        assertEquals("OldAlice", loaded.getUsernameHistory().get(0).getUsername());
        assertEquals(ts, loaded.getUsernameHistory().get(0).getAddedAt());
    }

    @Test
    public void usernameHistoryDeduplicatesByCaseInsensitiveUsernameOnSave() {
        long ts1 = 1000L;
        long ts2 = 2000L;
        Friend friend = new Friend("friend-dedup", "Bob", "code-dedup",
                "", "", "", false,
                Arrays.asList(
                        new UsernameHistoryEntry("Bobby", ts1),
                        new UsernameHistoryEntry("BOBBY", ts2)));
        FriendsRepository.saveFriend(friend);

        Friend loaded = FriendsRepository.getFriendById("friend-dedup");
        assertNotNull(loaded);
        assertEquals(1, loaded.getUsernameHistory().size());
        assertEquals("Bobby", loaded.getUsernameHistory().get(0).getUsername());
    }

    @Test
    public void deleteFriendAlsoDeletesUsernameHistory() {
        Friend friend = new Friend("friend-del-hist", "Carol", "code-del",
                "", "", "", false,
                Collections.singletonList(new UsernameHistoryEntry("OldCarol", 5000L)));
        FriendsRepository.saveFriend(friend);

        assertNotNull(FriendsRepository.getFriendById("friend-del-hist"));
        assertTrue(FriendsRepository.deleteFriend("friend-del-hist"));
        assertNull(FriendsRepository.getFriendById("friend-del-hist"));
    }

    private Friend findFriendByDisplayName(String displayName) {
        for (Friend friend : FriendsRepository.getFriends()) {
            if (displayName.equals(friend.getDisplayName())) {
                return friend;
            }
        }
        return null;
    }
}

