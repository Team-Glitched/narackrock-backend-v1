package glitched.adlips.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class LocalTestUserConfigurationTest {
    @Test
    void createsProfileForExistingLocalTestUser() throws Exception {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.create("local@example.com").withId(1L)));
        when(profiles.findByUserId(1L)).thenReturn(Optional.empty());

        new LocalTestUserConfiguration().localTestData(users, profiles, jdbcTemplate)
                .run(new DefaultApplicationArguments());

        verify(profiles).save(any(Profile.class));
        verify(jdbcTemplate).update(any(String.class));
    }
}
