package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EmailAuthTokenTest {

    @Test
    void createLoginTokenByDefaultPurpose() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        EmailAuthToken authToken = new EmailAuthToken("user@example.com", "token-value", expiresAt);

        assertEquals("user@example.com", authToken.getEmail());
        assertEquals("token-value", authToken.getToken());
        assertEquals(EmailAuthPurpose.LOGIN, authToken.getPurpose());
        assertEquals(expiresAt, authToken.getExpiresAt());
    }

    @Test
    void supportsSignupPurposeForFutureExpansion() {
        assertEquals(EmailAuthPurpose.SIGNUP, EmailAuthPurpose.valueOf("SIGNUP"));
    }

    @Test
    void mapEmailAuthTokensTableByErdColumns() throws NoSuchFieldException {
        Table table = EmailAuthToken.class.getAnnotation(Table.class);
        assertEquals("email_auth_tokens", table.name());

        assertEquals("email", column("email").name());
        assertEquals("token", column("token").name());
        assertEquals("purpose", column("purpose").name());
        assertEquals("expires_at", column("expiresAt").name());
        assertEquals("verified_at", column("verifiedAt").name());
    }

    private Column column(String fieldName) throws NoSuchFieldException {
        Field field = EmailAuthToken.class.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class);
    }
}
