package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.model.UserProfile;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UserProfileRepositoryTest {

    @After
    public void tearDown() {
        UserProfileRepository.resetForTesting();
    }

    @Test
    public void getCurrentUserCreatesRandomTwoWordUserName() {
        UserProfile profile = UserProfileRepository.getCurrentUser();

        assertNotNull(profile.getId());
        assertTrue(profile.getUserName().contains(" "));
        String[] words = profile.getUserName().split(" ");
        assertEquals(2, words.length);
        assertFalse(words[0].isEmpty());
        assertFalse(words[1].isEmpty());
    }

    @Test
    public void updateUserNameChangesNameButPreservesId() {
        UserProfile original = UserProfileRepository.getCurrentUser();

        UserProfile updated = UserProfileRepository.updateUserName("Nova Drift");

        assertEquals("Nova Drift", updated.getUserName());
        assertEquals(original.getId(), updated.getId());
    }

    @Test
    public void updateUserNameKeepsPreviousNameWhenBlank() {
        UserProfile original = UserProfileRepository.updateUserName("Quiet Otter");

        UserProfile updated = UserProfileRepository.updateUserName("   ");

        assertEquals(original.getUserName(), updated.getUserName());
        assertNotEquals("", updated.getUserName());
    }

    @Test
    public void getCurrentUserReloadsFromPreferencesAfterCacheClear() {
        UserProfile seeded = new UserProfile("persist-id", "Amber Falcon");
        UserProfileRepository.setCurrentUserForTesting(seeded);

        UserProfileRepository.clearCachedUserForTesting();
        UserProfile reloaded = UserProfileRepository.getCurrentUser();

        assertEquals(seeded.getId(), reloaded.getId());
        assertEquals(seeded.getUserName(), reloaded.getUserName());
    }
}
