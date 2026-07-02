package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createsActiveUserWithDefaultRole() {
        User user = User.create("user@example.com");

        assertNull(user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    void withdrawsUserAtRequestedTime() {
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 6, 30, 12, 0);

        User user = User.create("user@example.com").withId(1L).withdraw(withdrawnAt);

        assertFalse(user.isActive());
        assertEquals(withdrawnAt, user.getDeletedAt());
    }
}
