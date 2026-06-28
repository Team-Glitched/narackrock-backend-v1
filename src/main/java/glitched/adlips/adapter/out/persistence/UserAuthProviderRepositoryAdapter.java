package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class UserAuthProviderRepositoryAdapter implements UserAuthProviderRepository {

    private final UserAuthProviderJpaRepository jpaRepository;

    UserAuthProviderRepositoryAdapter(UserAuthProviderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserAuthProvider save(UserAuthProvider authProvider) {
        return jpaRepository.save(authProvider);
    }

    @Override
    public Optional<UserAuthProvider> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
        return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
        return jpaRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }
}
