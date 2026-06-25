package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class UserAuthProviderTest {

    @Test
    void createGoogleAuthProvider() {
        User user = new User("user@example.com");
        UserAuthProvider authProvider = new UserAuthProvider(user, AuthProvider.GOOGLE, "google-sub", true);

        assertEquals(user, authProvider.getUser());
        assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
        assertEquals("google-sub", authProvider.getProviderUserId());
        assertTrue(authProvider.isEmailVerified());
    }

    @Test
    void supportsEmailProviderForFutureExpansion() {
        assertEquals(AuthProvider.EMAIL, AuthProvider.valueOf("EMAIL"));
    }

    @Test
    void mapUserAuthProvidersTableByErdColumns() throws NoSuchFieldException {
        Table table = UserAuthProvider.class.getAnnotation(Table.class);
        assertEquals("user_auth_providers", table.name());

        assertEquals("provider", column("provider").name());
        assertFalse(column("provider").nullable());
        assertEquals("provider_user_id", column("providerUserId").name());
        assertFalse(column("providerUserId").nullable());
        assertEquals("email_verified", column("emailVerified").name());
        assertFalse(column("emailVerified").nullable());
    }

    private Column column(String fieldName) throws NoSuchFieldException {
        Field field = UserAuthProvider.class.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class);
    }
}
