package glitched.adlips.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PersistenceConflictTranslationTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private ProfileJpaRepository profileJpaRepository;

    @Mock
    private UserAuthProviderJpaRepository authProviderJpaRepository;

    @Test
    void translatesDuplicateEmailToAlreadyRegistered() {
        given(userJpaRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("duplicate email"));
        UserRepositoryAdapter adapter = new UserRepositoryAdapter(userJpaRepository);

        assertThatThrownBy(() -> adapter.save(new User("user@gmail.com")))
                .isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void translatesDuplicateNicknameToDomainConflict() {
        User user = new User("user@gmail.com");
        given(profileJpaRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("duplicate nickname"));
        ProfileRepositoryAdapter adapter = new ProfileRepositoryAdapter(profileJpaRepository);

        assertThatThrownBy(() -> adapter.save(new Profile(user, "taken")))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    @Test
    void translatesDuplicateGoogleProviderToAlreadyRegistered() {
        User user = new User("user@gmail.com");
        UserAuthProvider provider = new UserAuthProvider(user, AuthProvider.GOOGLE, "google-sub", true);
        given(authProviderJpaRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("duplicate provider"));
        UserAuthProviderRepositoryAdapter adapter =
                new UserAuthProviderRepositoryAdapter(authProviderJpaRepository);

        assertThatThrownBy(() -> adapter.save(provider))
                .isInstanceOf(AlreadyRegisteredException.class);
    }
}
