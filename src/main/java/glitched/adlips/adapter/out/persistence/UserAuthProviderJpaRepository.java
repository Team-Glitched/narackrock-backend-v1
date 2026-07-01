package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAuthProviderJpaRepository extends JpaRepository<UserAuthProvider, Long> {
    Optional<UserAuthProvider> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
