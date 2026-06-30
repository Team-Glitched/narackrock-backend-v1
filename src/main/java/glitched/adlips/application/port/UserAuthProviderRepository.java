package glitched.adlips.application.port;

import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;

public interface UserAuthProviderRepository {
    UserAuthProvider save(UserAuthProvider authProvider);
    Optional<UserAuthProvider> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
