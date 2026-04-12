package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Friend;
import com.kingjoshdavid.funfection.model.UserProfile;
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

        Friend friend = new Friend("friend-1", "Alpha");
        FriendsRepository.saveFriend(friend);

        Friend loaded = FriendsRepository.getFriendById("friend-1");
        assertNotNull(loaded);
        assertEquals("Alpha", loaded.getDisplayName());
        assertEquals("", loaded.getDisplayNameOverride());
    }

    @Test
    public void getFriendsReturnsNewestFirstAndUpdateKeepsSingleRow() throws Exception {
        Friend first = new Friend("friend-a", "A");
        Friend second = new Friend("friend-b", "B");

        FriendsRepository.saveFriend(first);
        Thread.sleep(2L);
        FriendsRepository.saveFriend(second);

        List<Friend> ordered = FriendsRepository.getFriends();
        assertEquals("friend-b", ordered.get(0).getId());
        assertEquals("friend-a", ordered.get(1).getId());

        FriendsRepository.saveFriend(new Friend("friend-a", "A Updated"));

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
        Friend first = new Friend("friend-pick-1", "One");
        Friend second = new Friend("friend-pick-2", "Two");
        FriendsRepository.saveFriend(first);
        FriendsRepository.saveFriend(second);

        List<Friend> picked = FriendsRepository.pickByIds(Arrays.asList("friend-pick-2", "missing", "friend-pick-1"));
        assertEquals(2, picked.size());
        assertEquals("friend-pick-2", picked.get(0).getId());
        assertEquals("friend-pick-1", picked.get(1).getId());

        assertTrue(FriendsRepository.deleteFriend("friend-pick-1"));
        Friend anonymized = FriendsRepository.getFriendById("friend-pick-1");
        assertNotNull(anonymized);
        assertEquals("Mysterious Entity", anonymized.getDisplayName());
        assertEquals("", anonymized.getDisplayNameOverride());
        assertTrue(anonymized.getUsernameHistory().isEmpty());
        assertTrue(FriendsRepository.deleteFriend("friend-pick-1"));
    }

    @Test
    public void saveFriendRoundTripsHistoryAndProtectedMetadata() {
        Friend scientist = new Friend("scientist-room", "Professor Tesla [SIMULATED]",
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
    public void initializeAddsCurrentUserButHidesSelfByDefault() {
        FriendsRepository.initialize(ApplicationProvider.getApplicationContext());

        UserProfile currentUser = UserProfileRepository.getCurrentUser();
        Friend storedSelf = FriendsRepository.getFriendById(currentUser.getId());
        assertNotNull(storedSelf);
        assertTrue(storedSelf.isProtectedProfile());

        List<Friend> visibleFriends = FriendsRepository.getFriends();
        assertNull(findFriendById(visibleFriends, currentUser.getId()));
    }

    @Test
    public void friendsListCanShowSelfWhenEnabled() {
        FriendsRepository.initialize(ApplicationProvider.getApplicationContext());
        UserProfile currentUser = UserProfileRepository.getCurrentUser();

        AppSettingsRepository.setCurrentUserVisibleInFriendsList(true);
        List<Friend> visibleFriends = FriendsRepository.getFriends();

        assertNotNull(findFriendById(visibleFriends, currentUser.getId()));
    }

    @Test
    public void updatingCurrentUserNameArchivesPreviousHandle() {
        UserProfileRepository.setCurrentUserForTesting(new UserProfile("self-user", "First Alias"));
        FriendsRepository.initialize(ApplicationProvider.getApplicationContext());

        UserProfileRepository.updateUserName("Second Alias");
        FriendsRepository.ensureCurrentUserFriendExists();

        Friend updatedSelf = FriendsRepository.getFriendById("self-user");
        assertNotNull(updatedSelf);
        assertEquals("Second Alias", updatedSelf.getDisplayName());
        assertEquals(1, updatedSelf.getUsernameHistory().size());
        assertEquals("First Alias", updatedSelf.getUsernameHistory().get(0).getUsername());

        UserProfileRepository.updateUserName("second alias");
        FriendsRepository.ensureCurrentUserFriendExists();

        Friend caseOnlyRename = FriendsRepository.getFriendById("self-user");
        assertNotNull(caseOnlyRename);
        assertEquals(1, caseOnlyRename.getUsernameHistory().size());
    }

    @Test
    public void simulatedScientistsAlwaysAppearLastInVisibleFriendsList() {
        FriendsRepository.initialize(ApplicationProvider.getApplicationContext());
        AppSettingsRepository.setCurrentUserVisibleInFriendsList(true);

        FriendsRepository.saveFriend(new Friend("friend-order-1", "Alpha",
                "", "", "", false, Collections.emptyList(), 100L));
        FriendsRepository.saveFriend(new Friend("friend-order-2", "Beta",
                "", "", "", false, Collections.emptyList(), 200L));

        List<Friend> visibleFriends = FriendsRepository.getFriends();
        assertTrue(visibleFriends.size() >= 8);

        int scientistCount = 0;
        boolean foundScientistBlock = false;
        for (Friend friend : visibleFriends) {
            boolean scientist = friend.isProtectedProfile()
                    && isKnownScientistName(friend.getDisplayName());
            if (scientist) {
                scientistCount++;
                foundScientistBlock = true;
                continue;
            }
            assertFalse(foundScientistBlock);
        }

        assertEquals(5, scientistCount);
        assertEquals("friend-order-2", visibleFriends.get(0).getId());
        assertEquals("friend-order-1", visibleFriends.get(1).getId());
    }

    @Test
    public void usernameHistoryTimestampPersistedAndReturnedOnFetch() {
        long ts = 999_000L;
        Friend friend = new Friend("friend-ts", "Alice",
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
        Friend friend = new Friend("friend-dedup", "Bob",
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
        Friend friend = new Friend("friend-del-hist", "Carol",
                "", "", "", false,
                Collections.singletonList(new UsernameHistoryEntry("OldCarol", 5000L)));
        FriendsRepository.saveFriend(friend);

        assertNotNull(FriendsRepository.getFriendById("friend-del-hist"));
        assertTrue(FriendsRepository.deleteFriend("friend-del-hist"));
        Friend anonymized = FriendsRepository.getFriendById("friend-del-hist");
        assertNotNull(anonymized);
        assertEquals("Mysterious Entity", anonymized.getDisplayName());
        assertTrue(anonymized.getUsernameHistory().isEmpty());
    }

    private Friend findFriendByDisplayName(String displayName) {
        for (Friend friend : FriendsRepository.getFriends()) {
            if (displayName.equals(friend.getDisplayName())) {
                return friend;
            }
        }
        return null;
    }

    private Friend findFriendById(List<Friend> friends, String id) {
        for (Friend friend : friends) {
            if (id.equals(friend.getId())) {
                return friend;
            }
        }
        return null;
    }

    private boolean isKnownScientistName(String displayName) {
        return "Professor Tesla".equals(displayName)
                || "Doctor Curie".equals(displayName)
                || "Doc Brown".equals(displayName)
                || "Professor Xavier".equals(displayName)
                || "The Gutter Man".equals(displayName);
    }
}

