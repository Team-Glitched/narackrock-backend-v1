package glitched.adlips.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class LocalTestUserConfigurationTest {
    @Test
    void createsProfilesForLocalTestUsers() throws Exception {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.create("local@example.com").withId(1L)));
        when(users.findByEmail("bookmark-observer@localhost"))
                .thenReturn(Optional.of(User.create("bookmark-observer@localhost").withId(2L)));
        when(profiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(profiles.findByUserId(2L)).thenReturn(Optional.empty());

        new LocalTestUserConfiguration().localTestData(users, profiles, jdbcTemplate)
                .run(new DefaultApplicationArguments());

        ArgumentCaptor<Profile> savedProfiles = ArgumentCaptor.forClass(Profile.class);
        verify(profiles, times(2)).save(savedProfiles.capture());
        assertThat(savedProfiles.getAllValues())
                .extracting(Profile::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        verify(jdbcTemplate, atLeastOnce()).update(any(String.class));
    }
}
