package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserAuthProviderTest {

    @Test
    void createsGoogleAuthProvider() {
        UserAuthProvider authProvider = UserAuthProvider.google(1L, "google-sub", true);

        assertEquals(1L, authProvider.getUserId());
        assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
        assertEquals("google-sub", authProvider.getProviderUserId());
        assertTrue(authProvider.isEmailVerified());
    }
}
