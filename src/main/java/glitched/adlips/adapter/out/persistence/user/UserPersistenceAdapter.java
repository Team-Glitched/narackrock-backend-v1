package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserPersistenceAdapter implements
        UserRepositoryPort,
        ProfileRepositoryPort,
        UserAuthProviderRepositoryPort {
    private final SpringDataUserRepository userRepository;
    private final SpringDataProfileRepository profileRepository;
    private final SpringDataUserAuthProviderRepository authProviderRepository;

    public UserPersistenceAdapter(
            SpringDataUserRepository userRepository,
            SpringDataProfileRepository profileRepository,
            SpringDataUserAuthProviderRepository authProviderRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.authProviderRepository = authProviderRepository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<Profile> findByUserId(Long userId) {
        return profileRepository.findById(userId);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return profileRepository.existsByNickname(nickname);
    }

    @Override
    public Profile save(Profile profile) {
        return profileRepository.save(profile);
    }

    @Override
    public Optional<UserAuthProvider> findByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    ) {
        return authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public UserAuthProvider save(UserAuthProvider authProvider) {
        return authProviderRepository.save(authProvider);
    }
}
