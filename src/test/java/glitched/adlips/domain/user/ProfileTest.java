package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProfileTest {

    @Test
    void createsProfileWithErdDefaults() {
        Profile profile = Profile.create(1L, "guitar_moon");

        assertEquals(1L, profile.getUserId());
        assertEquals("guitar_moon", profile.getNickname());
        assertNull(profile.getProfileImageFileId());
        assertFalse(profile.isPrivate());
        assertEquals(0, profile.getFollowerCount());
        assertEquals(0, profile.getFollowingCount());
        assertFalse(profile.isMuted());
    }

    @Test
    void togglingMuteFlipsCurrentState() {
        Profile profile = Profile.create(1L, "guitar_moon");

        profile.toggleMuted();
        assertTrue(profile.isMuted());

        profile.toggleMuted();
        assertFalse(profile.isMuted());
    }
}
