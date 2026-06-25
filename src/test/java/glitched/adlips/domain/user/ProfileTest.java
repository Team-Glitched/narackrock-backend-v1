package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ProfileTest {

    @Test
    void createProfileWithErdDefaults() {
        User user = new User("user@example.com");

        Profile profile = new Profile(user, "guitar_moon");

        assertEquals(user, profile.getUser());
        assertEquals("guitar_moon", profile.getNickname());
        assertNull(profile.getProfileImageFileId());
        assertFalse(profile.isPrivate());
        assertEquals(0, profile.getFollowerCount());
        assertEquals(0, profile.getFollowingCount());
    }

    @Test
    void mapProfilesTableByErdColumns() throws NoSuchFieldException {
        Table table = Profile.class.getAnnotation(Table.class);
        assertEquals("profiles", table.name());

        assertEquals("nickname", column("nickname").name());
        assertEquals("profile_image_file_id", column("profileImageFileId").name());
        assertEquals("explanation", column("explanation").name());
        assertEquals("primary_instrument", column("primaryInstrument").name());
        assertEquals("is_private", column("isPrivate").name());
        assertEquals("follower_count", column("followerCount").name());
        assertEquals("following_count", column("followingCount").name());
        assertEquals("nickname_updated_at", column("nicknameUpdatedAt").name());
    }

    private Column column(String fieldName) throws NoSuchFieldException {
        Field field = Profile.class.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class);
    }
}
