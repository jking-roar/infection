package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Friend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
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
}

