package glitched.adlips.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createUserWithEmailAndDefaultRole() {
        User user = new User("user@example.com");

        assertEquals("user@example.com", user.getEmail());
        assertEquals(UserRole.USER, user.getRole());
        assertNull(user.getDeletedAt());
    }

    @Test
    void mapUsersTableByErdColumns() throws NoSuchFieldException {
        Table table = User.class.getAnnotation(Table.class);
        assertEquals("users", table.name());
        assertTrue(BaseTimeEntity.class.isAssignableFrom(User.class));

        Column email = column("email");
        assertEquals("email", email.name());
        assertTrue(email.unique());
        assertFalse(email.nullable());

        Column role = column("role");
        assertEquals("role", role.name());
        assertFalse(role.nullable());

        assertEquals("deleted_at", column("deletedAt").name());
    }

    @Test
    void doesNotKeepAuthenticationProviderOnUser() {
        assertThrows(NoSuchFieldException.class, () -> User.class.getDeclaredField("authId"));
        assertThrows(NoSuchFieldException.class, () -> User.class.getDeclaredField("authProvider"));
    }

    private Column column(String fieldName) throws NoSuchFieldException {
        Field field = User.class.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class);
    }
}
