package glitched.adlips.application.user.account.port.out;

import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;

public interface UserAuthProviderRepositoryPort {
    Optional<UserAuthProvider> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    UserAuthProvider save(UserAuthProvider authProvider);
}
