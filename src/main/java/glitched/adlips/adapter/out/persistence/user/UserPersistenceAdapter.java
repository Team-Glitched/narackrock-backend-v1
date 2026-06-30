package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.port.out.ProfileQueryPort;
import glitched.adlips.application.user.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import glitched.adlips.application.user.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class UserPersistenceAdapter implements
        UserRepositoryPort,
        ProfileRepositoryPort,
        ProfileQueryPort,
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
    public boolean existsByNicknameAndUserIdNot(String nickname, Long userId) {
        return profileRepository.existsByNicknameAndUserIdNot(nickname, userId);
    }

    @Override
    public List<Profile> findAllByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return profileRepository.findAllByUserIdIn(userIds);
    }

    @Override
    public PageResult<Profile> search(String keyword, int page, int size) {
        Page<Profile> result = profileRepository
                .findByNicknameContainingIgnoreCaseOrPrimaryInstrumentContainingIgnoreCaseOrExplanationContainingIgnoreCase(
                        keyword, keyword, keyword, PageRequest.of(page, size)
                );
        return new PageResult<>(result.getContent(), result.getTotalElements());
    }

    @Override
    public PageResult<Profile> findPopularExcluding(Set<Long> excludedIds, int page, int size) {
        Page<Profile> result = profileRepository.findByUserIdNotInOrderByFollowerCountDesc(
                excludedIds, PageRequest.of(page, size)
        );
        return new PageResult<>(result.getContent(), result.getTotalElements());
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
